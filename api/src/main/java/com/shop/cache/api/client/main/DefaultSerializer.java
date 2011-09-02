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
package com.shop.cache.api.client.main;

import com.shop.util.chunked.ChunkedByteArray;
import com.shop.util.chunked.ChunkedByteArrayInputStream;
import com.shop.util.chunked.ChunkedByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The default serializer implementation
 *
 * @author Jordan Zimmerman
 */
class DefaultSerializer implements SCSerializer
{
	static class DeserializeFailure extends Exception
	{
		DeserializeFailure(String message)
		{
			super(message);
		}
	}

	@Override
	public SCDataBlock 		deserialize(ChunkedByteArray bytes) throws Exception
	{
		SCDataBlock						block = new SCDataBlock();
		block.data(bytes);

		ObjectInputStream 				in = new ObjectInputStream(new GZIPInputStream(new ChunkedByteArrayInputStream(bytes)));

		int			fileVersion = in.readInt();
		if ( fileVersion != FILE_VERSION )
		{
			throw new DeserializeFailure("Incorrect File Version: " + fileVersion);
		}

		block.versionNumber(in.readInt());
		block.ttl(in.readLong());
		block.key((String)in.readUTF());

		block.object(in.readObject());

		in.close();

		return block;
	}

	@Override
	public ChunkedByteArray serialize(SCDataBlock block) throws Exception
	{
		ChunkedByteArray				chunked = new ChunkedByteArray();
		ObjectOutputStream 				out = new ObjectOutputStream(new GZIPOutputStream(new ChunkedByteArrayOutputStream(chunked)));

		out.writeInt(FILE_VERSION);
		out.writeInt(block.getVersionNumber());
		out.writeLong(block.getTTL());
		out.writeUTF(block.getKey());

		out.writeObject(block.getObject());

		out.close();

		chunked.lock();
		return chunked;
	}

	private static final int			FILE_VERSION = 0x00010001;
}
