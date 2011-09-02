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

package com.shop.util.generic;

import java.io.OutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Input stream that is heartbeat aware.
 *
 * @author Jordan Zimmerman
 */
class GenericIOOutputStream extends OutputStream
{
	/**
	 * @param stream actual stream to write to. It's vital that this stream is buffered for performance reasons.
	 */
	GenericIOOutputStream(OutputStream stream)
	{
		fStream = stream;
		fEscapesEnabled = new AtomicBoolean(true);
	}

	void	disableEscapes()
	{
		fEscapesEnabled.set(false);
	}

	void	writeHeartbeat() throws IOException
	{
		if ( fEscapesEnabled.get() )
		{
			GenericIOConstants.writeHeartbeat(fStream);
			flush();
		}
	}

	@Override
	public void write(int b) throws IOException
	{
		if ( fEscapesEnabled.get() && GenericIOConstants.isEscape(b) )
		{
			GenericIOConstants.writeNullByte(fStream);
		}
		else
		{
			fStream.write(b);
		}
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		while ( len-- > 0 )
		{
			write(b[off++]);
		}
	}

	@Override
	public void flush() throws IOException
	{
		fStream.flush();
	}

	@Override
	public void close() throws IOException
	{
		fStream.close();
	}

	private final OutputStream 	fStream;
	private final AtomicBoolean	fEscapesEnabled;
}
