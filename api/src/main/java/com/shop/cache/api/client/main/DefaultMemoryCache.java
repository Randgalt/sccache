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

import com.shop.cache.api.client.io.SCManager;

/**
 * The default memory cache implementation. This implementation
 * is based on SoftReferences. It does not limit the number of
 * objects cached in memory. Instead it relies on the JVM to handle
 * the SoftReferences.
 *
 * @author Jordan Zimmerman
 */
class DefaultMemoryCache implements SCMemoryCache
{
	/**
	 * Link back to the manager being used
	 *
	 * @param manager the manager
	 */
	DefaultMemoryCache(SCManager manager)
	{
		fMap = new ImpDefaultMemoryCacheMap(manager);

		Thread		purgeThread = new Thread
		(
			new Runnable()
			{
				@Override
				public void run()
				{
					stalePurgeLoop();
				}
			},
			"DefaultMemoryCache purge thread"
		);
		purgeThread.setDaemon(true);
		purgeThread.start();
	}

	@Override
	public void clear()
	{
		fMap.clear();
	}

	@Override
	public void put(SCDataBlock block)
	{
		fMap.put(block);
	}

	@Override
	public SCDataBlock get(String key)
	{
		return fMap.get(key);
	}

	/**
	 * DefaultMemoryCache uses a background thread that periodically checks the TTL of
	 * objects in memory against the main cache. Stale objects are removed from memory
	 */
	private void	stalePurgeLoop()
	{
		for(;;)
		{
			try
			{
				Thread.sleep(STALE_PURGE_SLEEP_TICKS);
			}
			catch ( InterruptedException e )
			{
				Thread.currentThread().interrupt();
				break;
			}

			fMap.purgeStale();
		}
	}

	private static final int			STALE_PURGE_SLEEP_TICKS = 60 * 1000;	// 1 minute

	private final DefaultMemoryCacheMap				fMap;
}
