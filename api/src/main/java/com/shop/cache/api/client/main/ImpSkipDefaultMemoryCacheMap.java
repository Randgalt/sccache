package com.shop.cache.api.client.main;

import com.shop.cache.api.client.io.SCManager;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * <br>
 * (c)2009 by SHOP.COM<br>
 * All rights reserved worldwide<br>
 *
 * @author Jordan Zimmerman
 */
public class ImpSkipDefaultMemoryCacheMap implements DefaultMemoryCacheMap
{
	ImpSkipDefaultMemoryCacheMap(SCManager manager)
	{
		fManager = manager;
		fMemoryCache = new ConcurrentSkipListMap<String, SoftReference<SCDataBlock>>();
		fQueue = new ReferenceQueue<SCDataBlock>();
	}

	@Override
	public void clear()
	{
		removeOldReferences();

		fMemoryCache.clear();
	}

	@Override
	public void put(SCDataBlock block)
	{
		removeOldReferences();

		fMemoryCache.put(block.getKey(), new SoftReference<SCDataBlock>(block, fQueue));
	}

	@Override
	public SCDataBlock get(String key)
	{
		removeOldReferences();

		SoftReference<SCDataBlock> 		memoryBlockRef = fMemoryCache.get(key);
		return (memoryBlockRef != null) ? memoryBlockRef.get() : null;
	}

	@Override
	public void purgeStale()
	{
		for ( String key : fMemoryCache.keySet() )
		{
			SoftReference<SCDataBlock> 	memoryBlockRef = fMemoryCache.get(key);
			if ( memoryBlockRef != null )
			{
				SCDataBlock		block = memoryBlockRef.get();
				if ( block != null )
				{
					try
					{
						long 			ttl = fManager.getTTL(key);
						if ( (ttl == 0) || (ttl > block.getTTL()) )	// TTL has changed - purge from memory so that the correct copy is retrieved
						{
							fMemoryCache.remove(key);
						}
					}
					catch ( Exception e )
					{
						if ( fManager.getNotificationHandler() != null )
						{
							fManager.getNotificationHandler().notifyException("DefaultMemoryCache.stalePurgeLoop()", e);
						}
					}
				}
			}
		}
	}

	private void removeOldReferences()
	{
		Reference<? extends SCDataBlock> reference;
		while ( (reference = fQueue.poll()) != null )
		{
			SCDataBlock 						block = reference.get();
			if ( block != null )
			{
				fMemoryCache.remove(block.getKey());
			}
		}
	}

	private final SCManager 												fManager;
	private final ReferenceQueue<SCDataBlock> 								fQueue;
	private final ConcurrentSkipListMap<String, SoftReference<SCDataBlock>> fMemoryCache;
}
