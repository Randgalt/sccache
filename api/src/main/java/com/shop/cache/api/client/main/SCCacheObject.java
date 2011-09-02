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

import java.util.concurrent.atomic.AtomicReference;

/**
 * High-level caching API. Combined with the {@link SCCacheObjectBuilder} functor, makes easier the process of checking for an object
 * in the cache and creating a new one if not in the cache.<br><br>
 * The canonical form of use is:
<code><pre>
SCCacheObject<MyObject>		cacher = new SCCacheObject<MyObject>
(
	myCache,
	new SCDataBlock(myKey),
	new SCCacheObjectBuilder<MyObject>()
	{
		&#64;Override
		public MyObject buildObject() throws Exception
		{
			// ....
			return newMyObject;
		}
	}
);
 MyObject		obj = cacher.get();
</pre></code>
 *
 * @author Jordan Zimmerman
 */
public class SCCacheObject<T>
{
	/**
	 * @param cache the main cache instance
	 * @param block description of object to be cached (key, TTL, version)
	 * @param builder builder for creating new instances
	 */
	public SCCacheObject(SCCache cache, SCDataBlock block, SCCacheObjectBuilder<T> builder)
	{
		this(cache, block, builder, null);
	}

	/**
	 * @param cache the main cache instance
	 * @param block description of object to be cached (key, TTL, version)
	 * @param builder builder for creating new instances
	 * @param typeCheck if not null, is used to validate that the object coming out of the cache is the correct type. If the object
	 * cannot be cast to typeCheck the object is considered missing
	 */
	public SCCacheObject(SCCache cache, SCDataBlock block, SCCacheObjectBuilder<T> builder, Class<? extends T> typeCheck)
	{
		fCache = cache;
		fBlock = block;
		fBuilder = builder;
		fIsFromCache = false;
		fGetType = SCCache.GetTypes.MISSING;
		fForce = false;
		fTypeCheck = typeCheck;
	}

	/**
	 * Causes {@link #get()} to always create a new object, regardless if there's an existing object in the cache
	 *
	 * @return this
	 */
	public SCCacheObject<T>	force()
	{
		fForce = true;
		return this;
	}

	/**
	 * Perform the get. An attempt is made to retrieve the object from the cache. If not found or stale, the builder will be called to generate
	 * a new object. That object will then be put into the cache.
	 *
	 * @return the cached object or a newly created object
	 * @throws Exception errors
	 */
	public T			get() throws Exception
	{
		fGetType = SCCache.GetTypes.MISSING;

		T		object = null;
		if ( !fForce )
		{
			AtomicReference<SCCache.GetTypes> 	ref = new AtomicReference<SCCache.GetTypes>();
			object = (T)fCache.get(fBlock, ref);
			if ( fTypeCheck != null )
			{
				try
				{
					fTypeCheck.cast(object);
				}
				catch ( Exception e )
				{
					object = null;
					if ( fCache.getNotificationHandler() != null )
					{
						fCache.getNotificationHandler().notifyException("SCCacheObject.get()", e);
					}
				}
			}

			if ( object != null )
			{
				fGetType = ref.get();
			}
		}

		if ( object != null )
		{
			fIsFromCache = true;
		}
		else
		{
			object = fBuilder.buildObject();
			fBlock.object(object);

			fCache.put(fBlock);
		}

		return object;
	}

	/**
	 * Returns true if the object returned by {@link #get()} was from the cache (as opposed to being newly created).
	 * Only valid after {@link #get()} has been called
	 *
	 * @return true/false
	 */
	public boolean		isFromCache()
	{
		return fIsFromCache;
	}

	/**
	 * Returns the type of cache that occurred.
	 * Only valid after {@link #get()} has been called
	 *
	 * @return type
	 */
	public SCCache.GetTypes		getType()
	{
		return fGetType;
	}

	private final SCCache 					fCache;
	private final SCDataBlock 				fBlock;
	private final SCCacheObjectBuilder<T> 	fBuilder;
	private final Class<? extends T> 		fTypeCheck;
	private boolean							fIsFromCache;
	private SCCache.GetTypes				fGetType;
	private boolean 						fForce;
}
