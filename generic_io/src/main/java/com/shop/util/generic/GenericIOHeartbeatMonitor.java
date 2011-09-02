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

import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

/**
 * Utility for maintaining heartbeats between clients/servers
 *
 * @author Jordan Zimmerman
 */
class GenericIOHeartbeatMonitor
{
	static final GenericIOHeartbeatMonitor		instance = new GenericIOHeartbeatMonitor();

	void			addClient(GenericIOClientImpl<?> client)
	{
		fClients.add(client);
	}

	void			removeClient(GenericIOClientImpl<?> client)
	{
		fClients.remove(client);
	}

	void			interrupt()
	{
		fThread.interrupt();
	}

	private GenericIOHeartbeatMonitor()
	{
		fClients = Collections.newSetFromMap(new ConcurrentHashMap<GenericIOClientImpl<?>, Boolean>());

		fThread = new Thread
		(
			new Runnable()
			{
				@Override
				public void run()
				{
					runLoop();
				}
			}
		);
		fThread.setDaemon(true);
		fThread.start();
	}

	private void runLoop()
	{
		while ( !Thread.currentThread().isInterrupted() )
		{
			try
			{
				Thread.sleep(HEARTBEAT_SLEEP_TICKS);
			}
			catch ( InterruptedException e )
			{
				Thread.currentThread().interrupt();
				break;
			}

			for ( GenericIOClientImpl<?> client : fClients )
			{
				if ( (System.currentTimeMillis() - client.getLastReadTicks()) > MAX_HEARTBEAT_LAPSE )
				{
					try
					{
						client.close();
					}
					catch ( IOException e )
					{
						// ignore
					}
				}
				else if ( (System.currentTimeMillis() - client.getLastFlushTicks()) > HEARTBEAT_TICKS )
				{
					try
					{
						client.sendHeartbeat();
					}
					catch ( IOException e )
					{
						client.internalClose();
					}
				}
			}
		}
	}

	private static final int	MAX_HEARTBEAT_LAPSE = 5 * 60 * 1000;	// 5 minutes
	private static final int	HEARTBEAT_TICKS = MAX_HEARTBEAT_LAPSE / 3;
	private static final int	HEARTBEAT_SLEEP_TICKS = HEARTBEAT_TICKS / 2;

	private final Thread						fThread;
	private final Set<GenericIOClientImpl<?>> 	fClients;
}
