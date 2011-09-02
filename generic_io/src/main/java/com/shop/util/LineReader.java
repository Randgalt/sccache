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
package com.shop.util;

import java.io.*;

/**
 * A line reading wrapper that works with byte streams. Has the same sematics as {@link BufferedReader}<br>
 *
 * @author Jordan Zimmerman
 */
public class LineReader extends InputStream
{
	public LineReader(InputStream in)
	{
		fIn = in;
		fBuffer = new StringBuilder();
		fLastWasCR = false;
		fPushbackChar = 0;
	}

	@Override
	public synchronized int 			read() throws IOException
	{
		if ( fPushbackChar == 0 )
		{
			return streamRead();
		}
		else
		{
			return pushbackRead();
		}
	}

	/**
	 * copied from {@link BufferedReader#readLine()} 
	 *
	 * Reads a line of text.  A line is considered to be terminated by any one
	 * of a line feed ('\n'), a carriage return ('\r'), or a carriage return
	 * followed immediately by a linefeed.
	 *
	 * @return     A String containing the contents of the line, not including
	 *             any line-termination characters, or null if the end of the
	 *             stream has been reached
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	public synchronized String 		readLine() throws IOException
	{
		fBuffer.setLength(0);

		boolean		eof = false;
		for(;;)
		{
			int		b = read();
			if ( b == -1 )
			{
				eof = true;
				break;
			}

			if ( b == '\r' )
			{
				fLastWasCR = true;
				break;
			}

			if ( b == '\n' )
			{
				break;
			}
            fBuffer.append((char)(b & 0xFF));
		}
		return ((fBuffer.length() == 0) && eof) ? null : fBuffer.toString();
	}

	@Override
	public synchronized void 	close() throws IOException
	{
		super.close();
		fBuffer = null;
		fIn.close();
	}

	/**
	 * Push the given char so that the next() read will return this char
	 * @param c char to push back
	 */
	public synchronized void	pushback(int c)
	{
		fPushbackChar = c;
	}

	private int pushbackRead()
	{
		int		b = fPushbackChar;
		fPushbackChar = 0;
		return b;
	}

	private int streamRead() throws IOException
	{
		int				b = fIn.read();
		if ( fLastWasCR )
		{
			fLastWasCR = false;

			char	c = (char)(b & 0xff);
			if ( c == '\n' )
			{
				return read();
			}
		}

		return b;
	}

	private InputStream fIn;
	private StringBuilder fBuffer;
	private boolean fLastWasCR;
	private int fPushbackChar;
}
