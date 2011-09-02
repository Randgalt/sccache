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

import com.shop.util.chunked.ChunkedByteArray;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.InputStream;

/**
 * Manages individual DB records<br>
 *
 * @author Jordan Zimmerman
 */
class CCDB2Record
{
	/**
	 * Denotes a normal record
	 */
	static final byte		OPCODE_NORMAL_RECORD = (byte)0xAD;

	/**
	 * Denotes a deleted record
	 */
	static final byte		OPCODE_DELETED_RECORD = (byte)0xEB;

	/**
	 * Wrap an existing record
	 *
	 * @param file the file
	 * @param useCRCs true if CRCs are used
	 * @param address object's address
	 * @return record
	 */
	static CCDB2Record existingRecord(CCDB2File file, boolean useCRCs, long address)
	{
		 return new CCDB2Record(file, useCRCs, address);
	}

	/**
	 * Wrap a new record - space in the file will be allocated for the record, though the object is not yet written
	 *
	 * @param file the file
	 * @param useCRCs true if CRCs are used
	 * @param key object key
	 * @param bytes the object bytes
	 * @param groupSpecs groups it belongs to or null
	 * @return the record
	 * @throws IOException errors
	 */
	static CCDB2Record newRecord(CCDB2File file, boolean useCRCs, String key, ChunkedByteArray bytes, long[] groupSpecs) throws IOException
	{
		CCDB2Record 		record = new CCDB2Record(file, useCRCs, NULL_ADDRESS);
		record.fKey = key;
		record.fKeySize = key.length();
		record.fObject = bytes;
		record.fGroupSpecs = groupSpecs;
		record.fGroupSpecQty = (groupSpecs != null) ? groupSpecs.length : 0;
		record.fObjectSize = bytes.size();
		record.allocateRecord();
		return record;
	}

	/**
	 * Mark a record as deleted. This causes {@link #OPCODE_DELETED_RECORD} to be written at the object's address
	 *
	 * @throws IOException errors
	 */
	void markDeleted() throws IOException
	{
		CCDB2io 		io = getFile();
		try
		{
			io.writeByte(OPCODE_DELETED_RECORD);
			fIsDeletedRecord = true;
		}
		finally
		{
			releaseIO(io);
		}
	}

	/**
	 * Returns true if the saved CRC matches a new CRC generated on the object. If CRCs are turned off, though, true is always returned
	 *
	 * @return true/false
	 */
	boolean CRCsMatch()
	{
		boolean		result = true;
		if ( fUseCRCs )
		{
			result = (hash(fObject) == fCRC);
		}
		return result;
	}

	/**
	 * Returns true if this is a deleted record
	 *
	 * @return true/false
	 */
	boolean isDeletedRecord()
	{
		return fIsDeletedRecord;
	}

	/**
	 * Returns the key length
	 *
	 * @return key length
	 */
	int getKeySize()
	{
		return fKeySize;
	}

	/**
	 * Return the data/object length
	 *
	 * @return length
	 */
	int getObjectSize()
	{
		return fObjectSize;
	}

	/**
	 * Returns the number of associated groups or 0
	 *
	 * @return group qty
	 */
	int getGroupSpecQty()
	{
		return fGroupSpecQty;
	}

	/**
	 * Return the CRC (if CRCs are turned on)
	 *
	 * @return CRC or 0
	 */
	int getCRC()
	{
		return fCRC;
	}

	/**
	 * Return the key
	 *
	 * @return key
	 */
	String getKey()
	{
		return fKey;
	}

	/**
	 * Return the object data
	 *
	 * @return data
	 */
	ChunkedByteArray getObject()
	{
		return fObject;
	}

	/**
	 * Return the associated Group IDs or null
	 *
	 * @return IDs or null
	 */
	long[] getGroupSpecs()
	{
		return fGroupSpecs;
	}

	/**
	 * Returns true if this record is EOF record
	 *
	 * @return true/false
	 */
	boolean isAtDeadByte()
	{
		return fIsAtDeadByte;
	}

	/**
	 * Used by {@link CCDB2Record#load(CCDB2Driver, LoadMode)} to determine what parts to load
	 */
	enum LoadMode
	{
		/**
		 * Load the entire record
		 */
		ALL,

		/**
		 * Load only the sizes/key
		 */
		KEY_ONLY,

		/**
		 * Load only the size
		 */
		SIZES_ONLY
	}

	/**
	 * Writes an object to its allocated address. The associated record fields are updated with the new values.
	 *
	 * @param key the key
	 * @param object the data
	 * @param groupSpecs associated groups or null
	 * @throws IOException erros.
	 */
	void writeRecord(String key, ChunkedByteArray object, long[] groupSpecs) throws IOException
	{
		CCDB2io 		io = getFile();
		try
		{
			fKeySize = key.length();
			fObjectSize = object.size();
			fCRC = fUseCRCs ? hash(object) : 0;
			fKey = key;
			fIsDeletedRecord = false;
			fObject = object;
			fGroupSpecs = groupSpecs;
			fGroupSpecQty = (groupSpecs != null) ? groupSpecs.length : 0;

			writeSizes(io);
			io.write(getKeyBytes(fKey));
			if ( fGroupSpecs != null )
			{
				for ( long spec : fGroupSpecs )
				{
					io.writeLong(spec);
				}
			}

			final RandomAccessFile file = io.getUnderlyingFile();
			fObject.writeTo
			(
				new OutputStream()
				{
					@Override
					public void write(int b) throws IOException
					{
						file.write(b);
					}

					@Override
					public void write(byte b[]) throws IOException
					{
						file.write(b);
					}

					@Override
					public void write(byte b[], int off, int len) throws IOException
					{
						file.write(b, off, len);
					}
				}
			);
		}
		finally
		{
			releaseIO(io);
		}
	}

	/**
	 * Return the allocated address
	 *
	 * @return address
	 */
	long getAddress()
	{
		return fAddress;
	}

	/**
	 * Return what would be the address of the next record
	 *
	 * @return next record address
	 */
	long 	getNextAddress()
	{
		return fAddress + fRecordSize;
	}

	/**
	 * Load the record from disk and update fields as specified by the load mode.
	 *
	 * @param driver the driver
	 * @param mode load mode
	 * @throws IOException errors
	 */
	void			load(CCDB2Driver driver, LoadMode mode) throws IOException
	{
		CCDB2io 		io = getFile();
		try
		{
			loadSizes(io);
			if ( fIsAtDeadByte )
			{
				return;
			}

			if ( !fIsDeletedRecord )
			{
				if ( (mode == LoadMode.KEY_ONLY) || (mode == LoadMode.ALL) )
				{
					loadKey(io);
					loadGroupSpecs(io);

					if ( mode == LoadMode.ALL )
					{
						loadObject(driver, io);
					}
				}
			}
		}
		catch ( OutOfMemoryError dummy )
		{
			driver.log("OOM trying to load record at address: " + fAddress, null, true);
			fIsDeletedRecord = true;
			fIsAtDeadByte = true;
		}
		finally
		{
			releaseIO(io);
		}
	}

	static byte[] getKeyBytes(String key)
	{
		byte[]		b = new byte[key.length()];
		for ( int i = 0; i < b.length; ++i )
		{
			b[i] = (byte)(key.charAt(i) & 0xff);
		}
		return b;
	}

	static String readKey(CCDB2ReadFully file, int keySize) throws IOException
	{
		byte[]		buffer = new byte[keySize];
		file.readFully(buffer);
		return new String(buffer);
	}

	private static final long			NULL_ADDRESS = -1;

	private CCDB2Record(CCDB2File file, boolean useCRCs, long address)
	{
		fFile = file;
		fUseCRCs = useCRCs;
		fAddress = address;
		fSizesBuffer = new byte[getSizesSize()];
	}

	private int getSizesSize()
	{
		return
			1 +						// opcode
			4 + 					// record size
			4 + 					// key size
			4 + 					// object size
			4 + 					// group spec qty
			(fUseCRCs ? 4 : 0);	// crc
	}

	private void allocateRecord() throws IOException
	{
		assert (fKey.length() > 0) && (fObject.size() > 0);

		fRecordSize =
			getSizesSize() +
			fKey.length() + 		// key size int
			fObject.size() +		// object size int
			(fGroupSpecQty * 8);		// group specs
		fAddress = fFile.allocate(fRecordSize);
	}

	private void writeSizes(CCDB2io file) throws IOException
	{
		DataOutputStream	out = new DataOutputStream
		(
			new OutputStream()
			{
				@Override
				public void write(int b) throws IOException
				{
					fSizesBuffer[fIndex++] = (byte)(b & 0xff);
				}

				private int fIndex = 0;
			}
		);

		out.writeByte(fIsDeletedRecord ? OPCODE_DELETED_RECORD : OPCODE_NORMAL_RECORD);
		out.writeInt(fRecordSize);
		out.writeInt(fKeySize);
		out.writeInt(fObjectSize);
		out.writeInt(fGroupSpecQty);
		if ( fUseCRCs )
		{
			out.writeInt(fCRC);
		}

		file.write(fSizesBuffer);
	}

	private void loadSizes(CCDB2io file) throws IOException
	{
		try
		{
			file.readFully(fSizesBuffer);
		}
		catch ( EOFException dummy )
		{
			fIsAtDeadByte = true;
			return;
		}

		DataInputStream in = new DataInputStream(new ByteArrayInputStream(fSizesBuffer));

		byte			opcode = in.readByte();
		fIsAtDeadByte = (opcode == 0);
		if ( fIsAtDeadByte )
		{
			return;
		}

		if ( (opcode != OPCODE_NORMAL_RECORD) && (opcode != OPCODE_DELETED_RECORD) )
		{
			throw new IOException("File is corrupted at address: " + fAddress);
		}

		fIsDeletedRecord = (opcode == OPCODE_DELETED_RECORD);
		fRecordSize = in.readInt();
		if ( !fIsDeletedRecord )
		{
			fKeySize = in.readInt();
			fObjectSize = in.readInt();
			fGroupSpecQty = in.readInt();
			fCRC = fUseCRCs ? in.readInt() : 0;
		}
		else
		{
			fKeySize = 0;
			fObjectSize = 0;
			fCRC = 0;
			fGroupSpecQty = 0;
		}
	}

	private void loadKey(CCDB2io file) throws IOException
	{
		fKey = readKey(file, fKeySize);
	}

	private void loadGroupSpecs(CCDB2io file) throws IOException
	{
		if ( fGroupSpecQty > 0 )
		{
			fGroupSpecs = new long[fGroupSpecQty];
			for ( int i = 0; i < fGroupSpecQty; ++i )
			{
				fGroupSpecs[i] = file.readLong();
			}
		}
		else
		{
			fGroupSpecs = null;
		}
	}

	private void loadObject(CCDB2Driver driver, CCDB2io io) throws IOException
	{
		final RandomAccessFile 	file = io.getUnderlyingFile();

		if ( driver.doChunking() && (fObjectSize >= ChunkedByteArray.DEFAULT_CHUNK_SIZE) )
		{
			fObject = new ChunkedByteArray();
			fObject.append
			(
				new InputStream()
				{
					@Override
					public int read(byte b[]) throws IOException
					{
						return file.read(b);
					}

					@Override
					public int read(byte b[], int off, int len) throws IOException
					{
						return file.read(b, off, len);
					}

					@Override
					public int read() throws IOException
					{
						return file.read();
					}
				},
				fObjectSize
			);
		}
		else
		{
			byte[]			buffer = new byte[fObjectSize];
			io.readFully(buffer);
			fObject = ChunkedByteArray.wrap(buffer);
		}
	}

	private static int			hash(ChunkedByteArray bytes)
	{
		int			h = 0;
		for ( int i = 0; i < bytes.size(); ++i )
		{
			h = 31 * h + bytes.get(i);
		}

		return h;
	}

	private CCDB2io getFile() throws IOException
	{
		assert (fAddress > 0);

		CCDB2io		io = fFile.getFile();
		try
		{
			io.seek(fAddress);
		}
		catch ( IOException e )
		{
			releaseIO(io);
			throw e;
		}
		return io;
	}

	private void releaseIO(CCDB2io io)
	{
		fFile.releaseFile(io);
	}

	private	CCDB2File				fFile;
	private	boolean					fUseCRCs;
	private	long					fAddress;
	private	int						fKeySize;
	private	int						fObjectSize;
	private	int						fRecordSize;
	private	int						fGroupSpecQty;
	private	int						fCRC;
	private	String					fKey;
	private	boolean					fIsDeletedRecord;
	private	ChunkedByteArray 		fObject;
	private	long[]					fGroupSpecs;
	private	byte[]					fSizesBuffer;
	private	boolean					fIsAtDeadByte;
}