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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Replacement for {@link ByteArrayInputStream} that uses a {@link ChunkedByteArray} instead of a raw byte[].
 * <br>
 * 
 * <br>
 * <strong>Note that this implementation is not synchronized (unlike ByteArrayInputStream).</strong>
 * If multiple threads access a <code>ChunkedByteArrayInputStream</code> instance concurrently (or the managed ChunkedByteArray),
 * it <i>must</i> be synchronized externally.<br>
 *
 * @since 1.2 12/24/07 Made the offset and mark values volatile
 * <hr>
 * 1.1 Updated Javadoc<br>
 * @author Jordan Zimmerman &lt;jordan@jordanzimmerman.com&gt;
 * @see ChunkedByteArray
 */
public class ChunkedByteArrayInputStream extends InputStream
{
	/**
	 * Input over the given array
	 *
	 * @param array array
	 */
	public ChunkedByteArrayInputStream(ChunkedByteArray array)
	{
		this(array, 0, array.size());
	}

	/**
	 * Input over the given array at the given offset within the array up to the given length of the array
	 *
	 * @param array array
	 * @param offset offset into the array
	 * @param length the maximum number of bytes to read from the buffer.
	 */
	public ChunkedByteArrayInputStream(ChunkedByteArray array, int offset, int length)
	{
		fBytes = array;
		fOffset = 0;
		fSize = Math.min(offset + length, array.size());
		fMark = 0;
	}

	public boolean markSupported()
	{
		return true;
	}

	public void mark(int dummy)
	{
		fMark = fOffset;
	}

	public void reset()
	{
		fOffset = fMark;
	}

	public int read(byte b[])
	{
		return read(b, 0, b.length);
	}

	public int read(byte buffer[], int bufferOffet, int bufferLength)
	{
		int			thisLength = Math.min(available(), bufferLength);
		int 		bytesRead = (thisLength > 0) ? fBytes.get(fOffset, buffer, bufferOffet, thisLength) : -1;
		if ( bytesRead > 0 )
		{
			fOffset += bytesRead;
		}
		return bytesRead;
	}

	public int read()
	{
		return (fOffset < fSize) ? (fBytes.get(fOffset++) & 0xff) : -1;
	}

	public int available()
	{
		return fSize - fOffset;
	}

	public void close()
	{
		// NOP
	}

	int			getOffset()
	{
		return fOffset;
	}

	int			getSize()
	{
		return fSize;
	}

	ChunkedByteArray		getBytes()
	{
		return fBytes;
	}

	private final ChunkedByteArray 		fBytes;
	private final int					fSize;
	private volatile int				fOffset;
	private volatile int				fMark;
}
