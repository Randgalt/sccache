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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Output stream that is heartbeat aware. If the heartbeat escape is written, it is correctly escaped so that
 * {@link GenericIOInputStream} can catch it.
 *
 * @author Jordan Zimmerman
 */
class GenericIOInputStream extends InputStream
{
	interface HeartbeatReceivedNotifier
	{
		/**
		 * Called when a heartbeat is received
		 */
		public void 	heartbeatReceived();
	}

	/**
	 * @param stream actual stream to read from. It's vital that this stream is buffered for performance reasons.
	 * @param notifier optional notifier. Gets called if an heartbeat is received. Can be null
	 */
	GenericIOInputStream(InputStream stream, HeartbeatReceivedNotifier notifier)
	{
		fStream = stream;
		fHeartbeatNotifier = notifier;
		fEscapesEnabled = new AtomicBoolean(true);
	}

	void	disableEscapes()
	{
		fEscapesEnabled.set(false);
	}

	@Override
	public int read() throws IOException
	{
		boolean		done = false;
		int 		b = -1;
		while ( !done )
		{
			done = true;

			b = fStream.read();
			if ( fEscapesEnabled.get() && GenericIOConstants.isEscape(b) )
			{
				b = fStream.read();
				switch ( GenericIOConstants.getSecondByteType(b) )
				{
					case EOF:
					{
						b = -1;
						break;
					}

					case ERROR:
					{
						throw new IOException("Unexpected escaped byte: " + b);
					}

					case HEARTBEAT:
					{
						// read another byte - it's just the heartbeat
						done = false;
						break;
					}

					case ESCAPE:
					{
						b = GenericIOConstants.getEscape();
						break;
					}
				}
			}

			if ( fHeartbeatNotifier != null )
			{
				fHeartbeatNotifier.heartbeatReceived();	// any byte received suffices as a heartbeat. i.e. we still have a connection.
			}
		}

		return b;
	}

	@Override
	public void close() throws IOException
	{
		fStream.close();
	}

	private final InputStream 				fStream;
	private final HeartbeatReceivedNotifier fHeartbeatNotifier;
	private final AtomicBoolean 			fEscapesEnabled;
}
