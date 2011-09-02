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
import java.io.OutputStream;

/**
 * Replacement for {@link ByteArrayOutputStream} that uses a {@link ChunkedByteArray}. This stream is internally buffered.<br>
 *
 * <br>
 * <strong>Note that this implementation is not synchronized (unlike ByteArrayOutputStream).</strong>
 * If multiple threads access a <code>ChunkedByteArrayOutputStream</code> instance concurrently (or the managed ChunkedByteArray),
 * it <i>must</i> be synchronized externally.<br>
 *
 * @since 1.1 12/23/07 Updated Javadoc<br>
 * @author Jordan Zimmerman &lt;jordan@jordanzimmerman.com&gt;
 * @see ChunkedByteArray
 */
public class ChunkedByteArrayOutputStream extends OutputStream
{
	/**
	 * @param bytes the bytes to fill
	 */
	public ChunkedByteArrayOutputStream(ChunkedByteArray bytes)
	{
		fBytes = bytes;
		fBuffer = new byte[bytes.getChunkSize()];
		fBufferIndex = 0;
	}

	public void write(byte b[])
	{
		write(b, 0, b.length);
	}

	public void write(byte b[], int offset, int length)
	{
		while ( length > 0 )
		{
			int		thisSize = Math.min(fBuffer.length - fBufferIndex, length);
			if ( thisSize <= 0 )
			{
				flush();
				continue;
			}

			System.arraycopy(b, offset, fBuffer, fBufferIndex, thisSize);
			offset += thisSize;
			fBufferIndex += thisSize;
			length -= thisSize;
		}
	}

	public void write(int b)
	{
		if ( fBufferIndex >= fBuffer.length )
		{
			flush();
		}
		fBuffer[fBufferIndex++] = (byte)(b & 0xff);
	}

	/**
	 * Return the total size of the array
	 *
	 * @return the size
	 */
	public int size()
	{
		return fBytes.size() + fBufferIndex;
	}

	public void flush()
	{
		if ( fBufferIndex > 0 )
		{
			fBytes.append(fBuffer, 0, fBufferIndex);
			fBufferIndex = 0;
		}
	}

	public void close()
	{
		flush();
	}

	private final ChunkedByteArray		fBytes;
	private final byte[]				fBuffer;
	private int							fBufferIndex;
}
