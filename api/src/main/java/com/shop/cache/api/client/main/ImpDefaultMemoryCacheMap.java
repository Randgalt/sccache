package com.shop.cache.api.client.main;

import com.shop.cache.api.client.io.SCManager;
import java.lang.ref.SoftReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.Reference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Collections;

/**
 * <br>
 * (c)2009 by SHOP.COM<br>
 * All rights reserved worldwide<br>
 *
 * @author Jordan Zimmerman
 */
class ImpDefaultMemoryCacheMap implements DefaultMemoryCacheMap
{
	ImpDefaultMemoryCacheMap(SCManager manager)
	{
		fManager = manager;
		fMemoryCache = new ConcurrentHashMap<String, SoftReference<SCDataBlock>>();
		fMemoryCacheKeys = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		fQueue = new ReferenceQueue<SCDataBlock>();
	}

	@Override
	public void clear()
	{
		removeOldReferences();

		fMemoryCache.clear();
		fMemoryCacheKeys.clear();
	}

	@Override
	public void put(SCDataBlock block)
	{
		removeOldReferences();

		fMemoryCache.put(block.getKey(), new SoftReference<SCDataBlock>(block, fQueue));
		fMemoryCacheKeys.add(block.getKey());
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
		// calling ConcurrentHashMap.keySet() ends up locking references to much of the cache
		// which can cause an OutOfMemoryException. So, use a separate/parallel set for the keys
		for ( String key : fMemoryCacheKeys )
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
							fMemoryCacheKeys.remove(key);
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
				fMemoryCacheKeys.remove(block.getKey());
			}
		}
	}

	private final SCManager 											fManager;
	private final ReferenceQueue<SCDataBlock> 							fQueue;
	private final ConcurrentHashMap<String, SoftReference<SCDataBlock>>	fMemoryCache;
	private final Set<String> 											fMemoryCacheKeys;
}
