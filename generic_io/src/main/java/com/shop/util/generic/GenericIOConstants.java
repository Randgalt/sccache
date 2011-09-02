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

/**
 * Utilities
 *
 * @author Jordan Zimmerman
 */
class GenericIOConstants
{
	static boolean 		isEscape(int b)
	{
		return (byte)(b & 0xff) == ESCAPE_BYTE;
	}

	static boolean 		isEscape(byte b)
	{
		return b == ESCAPE_BYTE;
	}

	enum SecondByteType
	{
		ESCAPE,
		HEARTBEAT,
		ERROR,
		EOF
	}

	static int					getEscape()
	{
		//noinspection PointlessBitwiseExpression
		return ESCAPE_BYTE & 0xff;
	}

	static SecondByteType		getSecondByteType(int b)
	{
		if ( b < 0 )
		{
			return SecondByteType.EOF;
		}

		switch ( (byte)(b & 0xff) )
		{
			case NULL_BYTE:
			{
				return SecondByteType.ESCAPE;
			}

			case HEARTBEAT_BYTE:
			{
				return SecondByteType.HEARTBEAT;
			}

			default:
			{
				return SecondByteType.ERROR;
			}
		}
	}

	static void 			writeNullByte(OutputStream out) throws IOException
	{
		out.write(NULL_BYTE_BUFFER);
	}

	static void 			writeHeartbeat(OutputStream out) throws IOException
	{
		out.write(HEARTBEAT_BYTE_BUFFER);
	}

	private static final byte		ESCAPE_BYTE = (byte)0xFF;
	private static final byte		HEARTBEAT_BYTE = (byte)0xFE;
	private static final byte		NULL_BYTE = (byte)0xFD;

	private static final byte[] 	NULL_BYTE_BUFFER = {ESCAPE_BYTE, NULL_BYTE};
	private static final byte[] 	HEARTBEAT_BYTE_BUFFER = {ESCAPE_BYTE, HEARTBEAT_BYTE};
}
