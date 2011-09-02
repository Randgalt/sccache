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
package com.shop.util.chunked;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstracts an unbounded array that is internally broken into chunks. This avoids allocating large contiguous byte arrays
 * which is very inefficient in Java.
 * The ChunkedByteArray contrasts with JDK classes such as ByteBuffer, byte[], ByteArrayOutputStream, etc.
 * As those JDK classes get larger, they get more inefficient. In particular, ByteArrayOutputStream, must copy old data as it grows. Further,
 * the memory allocator in Java is not optimized for large objects. Smaller objects get allocated in the young heap and are allocated very fast.
 * The ChunkedByteArray takes advantage of this by only allocating relatively small objects but maintaining an arbitrarily large logical size to
 * clients of the class.<br>
 * <br>
 * The default chunk size is 32K. This can be changed by setting the system property <code>ChunkedByteArrayDefaultSize</code> to any number of bytes you like.<br>
 *
 * <br>
 * <strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a <code>ChunkedByteArray</code> instance concurrently,
 * and at least one of the threads modifies the array structurally (by calling {@link #clear()} or one of the
 * <code>append(...)</code> methods), it <i>must</i> be synchronized externally.<br>
 *
 * @since 1.3 1/30/08 rewrote several of the append() methods. Added an equals() implementation.
 * <hr>
 * 1.2 12/24/07 reworked offset calcs so that reads are thread-safe. I was originally worried about making an allocation to do it. But, as Brian Goetz says,
 * allocations can be as little as 10 instructions. My simple testing shows that the allocation in {@link #calcTempOffsets(int)} doesn't alter performance.<br>
 * <hr>
 * 1.1 12/23/07 append(InputStream in, Integer length) was incorrect if length was non-null. The read from the input stream could ask for too many bytes. Also, updated Javadoc.<br>
 * @author Jordan Zimmerman &lt;jordan@jordanzimmerman.com&gt;
 * @see ChunkedByteArrayInputStream
 * @see ChunkedByteArrayOutputStream
 */
public class ChunkedByteArray
{
	/**
	 * The default chunk size
	 */
	public static final int		DEFAULT_CHUNK_SIZE;

	/**
	 * General timing test. Writes 1 million bytes to both a ByteArrayOutputStream and a ChunkedByteArray. The write
	 * is done 100 times in succession. This test is done 4 times in random order. The timings for each try of 100 1 million
	 * byte writes is output to the console.
	 *
	 * @param args ignored
	 * @throws IOException errors
	 */
	public static void main(String[] args) throws IOException
	{
		Random			r = new Random();
		for ( int repetitions = 0; repetitions < 4; ++repetitions )
		{
			boolean[]		testOrder = r.nextBoolean() ? new boolean[]{false, true} : new boolean[]{true, false};

			System.out.println("Test #" + (repetitions + 1));
			for ( int tests = 0; tests < 2; ++tests )
			{
				long							ticks;

				if ( testOrder[tests] )
				{
					ticks = System.currentTimeMillis();
					for ( int i = 0; i < 100; ++i )
					{
						ChunkedByteArray				chunked = new ChunkedByteArray();
						OutputStream					chunkedOut = new ChunkedByteArrayOutputStream(chunked);
						for ( int j = 0; j < 1000000; ++j )
						{
							chunkedOut.write(0);
						}
						chunkedOut.flush();
					}
					System.out.println("Time to write 1 million bytes to a ChunkedByteArray 100 times:      " + (System.currentTimeMillis() - ticks) + " ms");
				}
				else
				{
					ticks = System.currentTimeMillis();
					for ( int i = 0; i < 100; ++i )
					{
						OutputStream					JDKOut = new ByteArrayOutputStream();
						for ( int j = 0; j < 1000000; ++j )
						{
							JDKOut.write(0);
						}
						JDKOut.flush();
					}
					System.out.println("Time to write 1 million bytes to a ByteArrayOutputStream 100 times: " + (System.currentTimeMillis() - ticks) + " ms");
				}
			}
			System.out.println();
		}
	}

	/**
	 * Returns a new ChunkedByteArray that wraps the given bytes. The chunk size will be set to the length of the byte array
	 *
	 * @param bytes array to wrap
	 * @return new ChunkedByteArray
	 */
	public static ChunkedByteArray		wrap(byte[] bytes)
	{
		ChunkedByteArray		cba = new ChunkedByteArray(bytes.length);
		cba.fBytes.add(bytes);
		cba.fLogicalSize = bytes.length;
		cba.fLocked.set(true);
		return cba;
	}

	/**
	 * Uses the default chunk size
	 */
	public ChunkedByteArray()
	{
		this(DEFAULT_CHUNK_SIZE);
	}

	/**
	 * @param chunkSize the chunk size to use
	 */
	public ChunkedByteArray(int chunkSize)
	{
		fChunkSize = chunkSize;
		fLogicalSize = 0;
		fBytes = new ArrayList<byte[]>();
		fLocked = new AtomicBoolean(false);
	}

	/**
	 * The total size of the array.
	 *
	 * @return the logical size of the array
	 */
	public int			size()
	{
		return fLogicalSize;
	}

	/**
	 * Reset this array so that the logical size is 0
	 */
	public void			clear()
	{
		checkLocked();

		fLogicalSize = 0;
		fBytes.clear();
	}

	/**
	 * Return the chunk size being used by this array
	 *
	 * @return chunk size
	 */
	public int			getChunkSize()
	{
		return fChunkSize;
	}

	/**
	 * Special purpose method. USE WITH CAUTION! Returns the first byte array used in the managed list of arrays.
	 *
	 * @return byte array or null
	 */
	public byte[]		unwrap()
	{
		return (fBytes.size() > 0) ? fBytes.get(0) : null;
	}

	/**
	 * Returns the byte at the given offset. IMPORTANT: this method is not very
	 * efficient. It's much better to read from a chunked array using {@link #get(int, byte[])} 
	 *
	 * @param offset offset
	 * @return byte
	 */
	public byte			get(int offset)
	{
		offsetsPOD		offsetsInfo = calcTempOffsets(offset);
		return fBytes.get(offsetsInfo.chunkIndex)[offsetsInfo.offsetWithinChunk];
	}

	/**
	 * Attempt to fill the given array starting at the given offset in the chunked array. The number of bytes actually filled is returned.
	 *
	 * @param offset offset within the chunked array
	 * @param bytes buffer to read into
	 * @return the number of bytes filled into the buffer or -1 if the offset is past the end of the chunked array
	 */
	public int			get(int offset, byte[] bytes)
	{
		return get(offset, bytes, 0, bytes.length);
	}

	/**
	 * Attempt to fill the given array starting at the given offset in the chunked array. The number of bytes actually filled is returned.
	 * 
	 * @param offset offset within the chunked array
	 * @param bytes buffer to read into
	 * @param bytesOffset offset within the given buffer to start filling
	 * @param bytesLength the maximum number of bytes to fill in the buffer
	 * @return the number of bytes filled into the buffer or -1 if the offset is past the end of the chunked array
	 */
	public int get(int offset, byte[] bytes, int bytesOffset, int bytesLength)
	{
		offsetsPOD	offsetsInfo = new offsetsPOD();
		int			bytesRead = -1;
		if ( offset < fLogicalSize )
		{
			while ( bytesLength > 0 )
			{
				calcTempOffsets(offset, offsetsInfo);
				if ( offsetsInfo.bytesAvailable == 0 )
				{
					break;
				}

				if ( bytesRead < 0 )
				{
					bytesRead = 0;
				}

				int		thisSize = Math.min(offsetsInfo.bytesAvailable, bytesLength);
				System.arraycopy(fBytes.get(offsetsInfo.chunkIndex), offsetsInfo.offsetWithinChunk, bytes, bytesOffset, thisSize);
				bytesRead += thisSize;
				bytesLength -= thisSize;
				offset += thisSize;
				bytesOffset += thisSize;
			}
		}
		return bytesRead;
	}

	/**
	 * Sets the byte at the given offset
	 *
	 * @param offset offset
	 * @param b the byte
	 */
	public void			set(int offset, byte b)
	{
		checkLocked();

		offsetsPOD		offsetsInfo = calcTempOffsets(offset);
		fBytes.get(offsetsInfo.chunkIndex)[offsetsInfo.offsetWithinChunk] = b;
	}

	/**
	 * Append a byte to the byte array growing the logical size of the array by one. IMPORTANT: this method is not very
	 * efficient. It's much better to append to a chunked array using {@link #append(byte[])} 
	 *
	 * @param b the byte to append
	 */
	public void			append(byte b)
	{
		checkLocked();

		offsetsPOD		offsetsInfo = calcTempOffsets(fLogicalSize);
		ensureCapacity(offsetsInfo);

		fBytes.get(offsetsInfo.chunkIndex)[offsetsInfo.offsetWithinChunk] = b;
		++fLogicalSize;
	}

	/**
	 * Append the given bytes. The logical size of the array is increased by the length of the bytes appended.
	 *
	 * @param bytes the bytes
	 */
	public void			append(byte[] bytes)
	{
		append(bytes, 0, bytes.length);
	}

	/**
	 * Append the given bytes. The logical size of the array is increased by length.
	 *
	 * @param bytes the bytes
	 * @param offset offset within the bytes to append
	 * @param length number of bytes to append
	 */
	public void			append(byte[] bytes, int offset, int length)
	{
		checkLocked();

		offsetsPOD		offsetsInfo = new offsetsPOD();
		while ( length > 0 )
		{
			calcTempOffsets(fLogicalSize, offsetsInfo);
			ensureCapacity(offsetsInfo);

			int		thisSize = Math.min(length, fChunkSize - offsetsInfo.offsetWithinChunk);
			System.arraycopy(bytes, offset, fBytes.get(offsetsInfo.chunkIndex), offsetsInfo.offsetWithinChunk, thisSize);

			fLogicalSize += thisSize;
			offset += thisSize;
			length -= thisSize;
		}
	}

	/**
	 * Append the given bytes. The logical size of the array is increased by length.
	 *
	 * @param fromBytes the bytes
	 * @param offset offset within the bytes to append
	 * @param length number of bytes to append
	 */
	public void			append(ChunkedByteArray fromBytes, int offset, int length)
	{
		checkLocked();

		offsetsPOD		offsetsInfo = new offsetsPOD();
		while ( length > 0 )
		{
			fromBytes.calcTempOffsets(offset, offsetsInfo);

			byte[]			bytes = fromBytes.fBytes.get(offsetsInfo.chunkIndex);
			int				bytesSize = Math.min(fromBytes.fChunkSize, fromBytes.fLogicalSize - offset);
			int				thisSize = Math.min(length, bytesSize);
			append(bytes, offsetsInfo.offsetWithinChunk, thisSize);

			offset += thisSize;
			length -= thisSize;
		}
	}

	/**
	 * Append from the given stream. The logical size of the array is increased. If length is null, the stream is read until EOF is
	 * read. Otherwise the stream is read for length bytes.<br>
	 * NOTE: This method is optimized to read from a {@link ChunkedByteArrayInputStream}.
	 *
	 * @param in the stream
	 * @param length number of bytes to read or, if null, read all bytes
	 * @throws IOException errors or if EOF is received before length bytes are read
	 */
	public void			append(InputStream in, Integer length) throws IOException
	{
		checkLocked();

		if ( in instanceof ChunkedByteArrayInputStream )
		{
			ChunkedByteArrayInputStream 		bytesIn = (ChunkedByteArrayInputStream)in;
			append(bytesIn.getBytes(), bytesIn.getOffset(), (length != null) ? Math.min(length, bytesIn.getSize()) : bytesIn.getSize());
		}
		else
		{
			offsetsPOD	offsetsInfo = new offsetsPOD();
			int			bytesRead = 0;
			for(;;)
			{
				if ( (length != null) && (bytesRead >= length) )
				{
					break;
				}
				calcTempOffsets(fLogicalSize, offsetsInfo);
				ensureCapacity(offsetsInfo);

				int 	unusedSpaceWithinChunk = fChunkSize - offsetsInfo.offsetWithinChunk;
				int		thisMaxSize = (length != null) ? Math.min(length - bytesRead, unusedSpaceWithinChunk) : unusedSpaceWithinChunk;
				int		thisBytesRead = in.read(fBytes.get(offsetsInfo.chunkIndex), offsetsInfo.offsetWithinChunk, thisMaxSize);
				if ( thisBytesRead < 0 )
				{
					if ( length != null )
					{
						throw new EOFException();
					}
					break;
				}

				fLogicalSize += thisBytesRead;
				bytesRead += thisBytesRead;
			}
		}
	}

	/**
	 * Write this array to the given stream
	 *
	 * @param out stream to write to
	 * @throws IOException errors
	 */
	public void			writeTo(OutputStream out) throws IOException
	{
		int			written = 0;
		for ( byte[] b : fBytes )
		{
			int		thisSize = Math.min(fChunkSize, fLogicalSize - written);
			if ( thisSize > 0 )
			{
				out.write(b, 0, thisSize);
				written += thisSize;
			}
			else
			{
				break;
			}
		}
	}

	/**
	 * If o is a ChunkedByteArray, compare all the data and return true if equal
	 *
	 * @param o object to compare against
	 * @return true/false
	 */
	@Override
	public boolean equals(Object o)
	{
		if ( this == o )
		{
			return true;
		}

		if ( (o == null) || (getClass() != o.getClass()) )
		{
			return false;
		}

		ChunkedByteArray 		rhs = (ChunkedByteArray)o;
		if ( fLogicalSize != rhs.fLogicalSize )
		{
			return false;
		}

		offsetsPOD		offsetsInfo = new offsetsPOD();
		offsetsPOD 		rhsOffsetsInfo = new offsetsPOD();
		int				localOffset = 0;
		while ( localOffset < fLogicalSize )
		{
			calcTempOffsets(localOffset, offsetsInfo);
			rhs.calcTempOffsets(localOffset, rhsOffsetsInfo);

			int				thisSize = Math.min(fLogicalSize - localOffset, Math.min(fChunkSize - offsetsInfo.offsetWithinChunk, rhs.fChunkSize - rhsOffsetsInfo.offsetWithinChunk));

			byte[]			bytes = fBytes.get(offsetsInfo.chunkIndex);
			byte[]			rhsBytes = rhs.fBytes.get(rhsOffsetsInfo.chunkIndex);
			for ( int i = 0; i < thisSize; ++i )
			{
				if ( bytes[offsetsInfo.offsetWithinChunk + i] != rhsBytes[rhsOffsetsInfo.offsetWithinChunk + i] )
				{
					return false;
				}
			}
			localOffset += thisSize;
		}

		return true;
	}

	/**
	 * Locks this array. Any attempt to modify it will throw {@link IllegalAccessError}
	 */
	public void			lock()
	{
		fLocked.set(true);
	}

	/**
	 * Called to make sure there is an extra buffer available for an append operation
	 *
	 * @param offsetsInfo calculated offsets
	 */
	private void 		ensureCapacity(offsetsPOD offsetsInfo)
	{
		if ( offsetsInfo.chunkIndex >= fBytes.size() )
		{
			fBytes.add(new byte[fChunkSize]);
		}
	}

	/**
	 * POD for holding offset calculations
	 */
	private static class offsetsPOD
	{
		/**
		 * index within the chunk array of the offset
		 */
		int						chunkIndex = 0;

		/**
		 * index within the calculated chunk of the offset
		 */
		int						offsetWithinChunk = 0;

		/**
		 * the unused bytes in the calculated chunk 
		 */
		int						bytesAvailable = 0;
	}

	/**
	 * Allocates a new offsets POD and calculate the values
	 *
	 * @param offset offset to calculate
	 * @return the offsets POD
	 */
	private offsetsPOD		calcTempOffsets(int offset)
	{
		offsetsPOD offsetsInfo = new offsetsPOD();
		calcTempOffsets(offset, offsetsInfo);
		return offsetsInfo;
	}

	/**
	 * Fill an offsets POD with the offset calculation
	 *
	 * @param offset the offset to calculate
	 * @param useOffsets the offsets POD
	 */
	private void			calcTempOffsets(int offset, offsetsPOD useOffsets)
	{
		useOffsets.chunkIndex = offset / fChunkSize;
		useOffsets.offsetWithinChunk = offset - (fChunkSize * useOffsets.chunkIndex);
		useOffsets.bytesAvailable = ((useOffsets.chunkIndex + 1) == fBytes.size()) ? (fLogicalSize - offset) : (fChunkSize - useOffsets.offsetWithinChunk);
	}

	private void checkLocked()
	{
		if ( fLocked.get() )
		{
			throw new IllegalStateException("The array has been locked");
		}
	}

	static
	{
		int 		defaultSize = 0x8000;	// 32K
		String 		s = System.getProperty("ChunkedByteArrayDefaultSize");
		if ( s != null )
		{
			try
			{
				defaultSize = Integer.parseInt(s);
			}
			catch ( NumberFormatException e )
			{
				// ignore
			}
		}
		DEFAULT_CHUNK_SIZE = defaultSize;
	}

	private final List<byte[]>		fBytes;
	private final int				fChunkSize;
	private final AtomicBoolean 	fLocked;
	private int						fLogicalSize;
}
