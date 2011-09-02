/*
 * Copyright 2008-2009 SHOP.COM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shop.util.ccdb2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;

/**
 * Manages the DB file. This class is, essentially, a very quick disk space allocator.<br>
 *
 * @author Jordan Zimmerman
 */
class CCDB2File
{

	/**
	 * @param driver the driver
	 * @param filePath full path to the file to manage
	 * @param nullByte value to store in newly allocated file space.
	 * @throws IOException errors
	 */
	CCDB2File(CCDB2Driver driver, String filePath, byte nullByte) throws IOException
	{
		long		allocationChunkSize = driver.getAllocationChunkSize();

		File 		localFle = new File(filePath);
		fFilePool = new CCDB2FilePool(driver, localFle);
		fActualSize = localFle.length();
		fLogicalSize = fActualSize;
		fAllocationLock = new Object();
		fAllocationChunkSize = (allocationChunkSize > 0) ? allocationChunkSize : DEFAULT_ALLOCATION_CHUNK_SIZE;

		fNullBuffer = new byte[NULL_BUFFER_SIZE];
		for ( int i = 0; i < NULL_BUFFER_SIZE; ++i )
		{
			fNullBuffer[i] = nullByte;
		}
	}

	/**
	 * Return the current number of users of this file
	 *
	 * @return user count
	 */
	int getFilePoolSize()
	{
		return fFilePool.getTotalQty();
	}

	/**
	 * Returns the chunk size being used. i.e. the minimum number of bytes allocated
	 *
	 * @return chunk size
	 */
	long getAllocationChunkSize()
	{
		return fAllocationChunkSize;
	}

	/**
	 * close the file
	 */
	void close()
	{
		fFilePool.close();
	}

	/**
	 * Return the file's size as client's would see it. i.e., if you allocate 1 byte, you will actually
	 * be allocating {@link #getAllocationChunkSize()} bytes. However, this method will still return 1.
	 *
	 * @return the logical size
	 */
	long getLogicalSize()
	{
		return fLogicalSize;
	}

	/**
	 * Returns the actual size of the file on disk (including unused space in an over-allocated chunk).
	 *
	 * @return actualy size
	 */
	long getActualSize()
	{
		return fActualSize;
	}

	/**
	 * Return an IO object to use for the file. This call MUST be balanced by a call to {@link #releaseFile(CCDB2io)}. If you do not
	 * call release() you will have a resource leak. 
	 *
	 * @return the I/O
	 * @throws IOException errors
	 */
	CCDB2io getFile() throws IOException
	{
		final RandomAccessFile 		f = fFilePool.get();
		return new CCDB2io()
		{
			@Override
			public void seek(long i) throws IOException
			{
				f.seek(i);
			}

			@Override
			public void writeByte(byte b) throws IOException
			{
				f.writeByte(b);
			}

			@Override
			public void writeInt(int i) throws IOException
			{
				f.writeInt(i);
			}

			@Override
			public void writeBoolean(boolean b) throws IOException
			{
				f.writeBoolean(b);
			}

			@Override
			public void writeLong(long l) throws IOException
			{
				f.writeLong(l);
			}

			@Override
			public byte readByte() throws IOException
			{
				return f.readByte();
			}

			@Override
			public int readInt() throws IOException
			{
				return f.readInt();
			}

			@Override
			public boolean readBoolean() throws IOException
			{
				return f.readBoolean();
			}

			@Override
			public long readLong() throws IOException
			{
				return f.readLong();
			}

			@Override
			public void write(byte[] bytes, int length) throws IOException
			{
				f.write(bytes, 0, length);
			}

			@Override
			public void write(byte[] b) throws IOException
			{
				f.write(b);
			}

			@Override
			public void readFully(byte[] b) throws IOException
			{
				f.readFully(b);
			}

			@Override
			public RandomAccessFile getUnderlyingFile()
			{
				return f;
			}
		};
	}

	/**
	 * Release an IO object returned from {@link #getFile()}
	 *
	 * @param file the file
	 */
	void releaseFile(CCDB2io file)
	{
		fFilePool.release(file.getUnderlyingFile());
	}

	/**
	 * Used internally when loading the file to strip off excess
	 *
	 * @param size size to truncate at
	 */
	void setLogicalSize(long size)
	{
		synchronized(fAllocationLock)
		{
			fLogicalSize = size;
		}
	}

	/**
	 * Allocates space in the file and returns an address to that space.
	 *
	 * @param amount the amount to allocate
	 * @return the address
	 * @throws FileNotFoundException no such file
	 * @throws CCDB2SetFileLengthException ran out of disk space or a similar error
	 */
	long			allocate(int amount) throws CCDB2SetFileLengthException, FileNotFoundException
	{
		long				offset;
		RandomAccessFile	file = null;
		try
		{
			synchronized(fAllocationLock)
			{
				offset = fLogicalSize;

				long newLogicalSize = offset + amount;
				if ( newLogicalSize > fActualSize )
				{
					long 				needed = newLogicalSize - fActualSize;
					long 				addAmount = quantize(needed, fAllocationChunkSize);

					file = fFilePool.get();
					long 				fileLength;
					try
					{
						nullNewFile(file, fActualSize, addAmount);
						fileLength = file.length();
					}
					catch ( IOException e )
					{
						throw new CCDB2SetFileLengthException(e.getMessage());
					}

					fActualSize += addAmount;
					assert fActualSize == fileLength;
				}
				fLogicalSize = newLogicalSize;
			}
		}
		finally
		{
			if ( file != null )
			{
				fFilePool.release(file);
			}
		}

		return offset;
	}

	private static long quantize(long value, long quantizeSize)
	{
		return ((value / quantizeSize) + 1) * quantizeSize;
	}

	private void nullNewFile(RandomAccessFile file, long offset, long size) throws IOException
	{
		file.seek(offset);
		while ( size > 0 )
		{
			if ( size < fNullBuffer.length )
			{
				int intSize = (int)size;
				file.write(fNullBuffer, 0, intSize);
				size -= intSize;
			}
			else
			{
				file.write(fNullBuffer);
				size -= fNullBuffer.length;
			}
		}

		if ( size < 0 )
		{
			throw new IOException("size went negative in nullNewFile(): " + size);
		}
	}

	private static final long 	DEFAULT_ALLOCATION_CHUNK_SIZE = 0x1000000;	// 16 MB

	private static final int	NULL_BUFFER_SIZE = 0x100000;

	private final Object		fAllocationLock;
	private final byte[]		fNullBuffer;
	private final long 			fAllocationChunkSize;

	private	CCDB2FilePool		fFilePool;
	private volatile long		fLogicalSize;
	private volatile long		fActualSize;
}