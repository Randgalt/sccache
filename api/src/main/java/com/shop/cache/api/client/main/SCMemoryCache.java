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

/**
 * Interface for the local, in-process cache
 *
 * @author Jordan Zimmerman
 */
public interface SCMemoryCache
{
	/**
	 * Put the given block into the memory cache. NOTE: ownership is shared with the main SCCache instance.
	 *
	 * @param block the block
	 */
	public void			put(SCDataBlock block);

	/**
	 * Return the block associated with the given key.
	 *
	 * @param key the key
	 * @return the block or null. NOTE: ownership is shared with the main SCCache instance.
	 */
	public SCDataBlock	get(String key);

	/**
	 * Purge any objects in memory
	 */
	public void			clear();
}
