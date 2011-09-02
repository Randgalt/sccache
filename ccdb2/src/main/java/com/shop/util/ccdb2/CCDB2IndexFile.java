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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a DB's companion index file<br>
 *
 * @author Jordan Zimmerman
 */
class CCDB2IndexFile
{
	/**
	 * Create a new index file
	 *
	 * @param driver the driver
	 * @param filePath Directory to store the file
	 * @throws IOException errors
	 */
	CCDB2IndexFile(CCDB2Driver driver, File filePath) throws IOException
	{
		fFilePath = filePath;
		fFile = new CCDB2File(driver, fFilePath.getPath(), NULL_BYTE);
	}

	/**
	 * Return the path passed to the constructor
	 *
	 * @return the path
	 */
	File getFilePath()
	{
		return fFilePath;
	}

	/**
	 * Close the index
	 */
	synchronized void		close()
	{
		fFile.close();
	}

	/**
	 * Add a new index entry
	 *
	 * @param key key
	 * @param address address in the DB of the object
	 * @param TTLDelta the time in the future that the object should be considered stale or {@link Long#MAX_VALUE}
	 * @param groupSpecs associated group IDs or null
	 * @throws IOException errors
	 */
	void addNewEntry(String key, long address, int TTLDelta, long[] groupSpecs) throws IOException
	{
		ByteArrayOutputStream		bytes = new ByteArrayOutputStream();
		DataOutputStream			out = new DataOutputStream(bytes);

		out.writeByte(MAGIC_BYTE);
		out.writeInt(MAGIC_INT);
		out.writeInt(key.length());
		out.write(CCDB2Record.getKeyBytes(key));
		out.writeLong(address);
		out.writeInt(TTLDelta);
		if ( groupSpecs != null )
		{
			out.writeInt(groupSpecs.length);
			for ( long spec : groupSpecs )
			{
				out.writeLong(spec);
			}
		}
		else
		{
			out.writeInt(0);
		}
		out.close();

		byte[]						entry = bytes.toByteArray();
		long 						entryAddress = fFile.allocate(entry.length);
		CCDB2io 					io = fFile.getFile();
		try
		{
			io.seek(entryAddress);
			io.write(entry);
		}
		finally
		{
			fFile.releaseFile(io);
		}
	}

	/**
	 * Load the index into memory. IMPORTANT, CCDB2 indexes are always completely in memory
	 *
	 * @param index the HashMap to store index entries in
	 * @param groupsIndex the HashMap to store groups in
	 * @param percentDone value to update with load percentage. As the index is loaded, this object will get increment as the load-percentage changes
	 * @param updateIndex callback so that clients can keep track of the size of the index in memory
	 * @throws IOException errors
	 */
	synchronized void 		load(ConcurrentHashMap<String, CCDB2IndexEntry> index, ConcurrentHashMap<Long, HashSet<String>> groupsIndex, final AtomicInteger percentDone, final CCDB2UpdateIndexInterface updateIndex) throws IOException
	{
		if ( !fFilePath.exists() )
		{
			percentDone.set(100);
			return;
		}

		final DataInputStream 	in = new DataInputStream(new BufferedInputStream(new FileInputStream(fFilePath), 0x10000));	// 1MB buffer
		//noinspection CaughtExceptionImmediatelyRethrown
		CCDB2ReadFully 			readFully = new CCDB2ReadFully()
		{
			@Override
			public void readFully(byte[] buffer) throws IOException
			{
				int 			bufferLength = buffer.length;
				int				offset = 0;
				while ( bufferLength > 0 )
				{
					int 	thisSize = Math.min(bufferLength, buffer.length);
					int 	thisRead = in.read(buffer, offset, thisSize);
					if ( thisRead < 0 )
					{
						throw new EOFException();
					}
					bufferLength -= thisRead;
					offset += thisRead;
				}
			}
		};
		try
		{
			long 				fileLength = Math.max(fFilePath.length(), 1);	// avoid divide by zero
			long 				currentOffset = 0;

			outer: for(;;)
			{
				boolean		checking = true;
				while ( checking )
				{
					int 			i = in.read();
					if ( i < 0 )
					{
						break outer;
					}

					byte			b = (byte)(i & 0xff);
					switch ( b )
					{
						case NULL_BYTE:
						{
							fFile.setLogicalSize(currentOffset);
							break outer;
						}

						case MAGIC_BYTE:
						{
							checking = false;
							break;
						}

						default:
						{
							throw new IOException("! Index is corrupt - bad magic byte");
						}
					}
					++currentOffset;
				}

				if ( in.readInt() != MAGIC_INT )
				{
					throw new IOException("! Index is corrupt - bad magic int");
				}
				currentOffset += 4;

				int 					keyLength = in.readInt();
				String					key = CCDB2Record.readKey(readFully, keyLength);
				long					address = in.readLong();
				int 					TTLDelta = in.readInt();
				int 					groupSpecsQty = in.readInt();
				currentOffset += 4 + key.length() + 8 + 4 + 4;

				while ( groupSpecsQty-- > 0 )
				{
					CCDB2Instance.addToGroup(groupsIndex, key, in.readLong());
					currentOffset += 8;
				}

				CCDB2IndexEntry		entry = new CCDB2IndexEntry();
				entry.address = address;
				entry.TTLDelta = TTLDelta;
				index.put(key, entry);
				updateIndex.updateIndexSize(key, true);

				percentDone.set((int)((currentOffset * 100) / fileLength));
			}
		}
		finally
		{
			in.close();
		}

		percentDone.set(100);
	}

	private static final int 		MAGIC_INT = 0xCAFEBABE;
	private static final byte		MAGIC_BYTE = (byte)0xBF;
	private static final byte		NULL_BYTE = (byte)0;

	private final File			fFilePath;
	private final CCDB2File		fFile;
}