package com.shop.cache.api.client.main;

/**
 * <br>
 * (c)2009 by SHOP.COM<br>
 * All rights reserved worldwide<br>
 *
 * @author Jordan Zimmerman
 */
interface DefaultMemoryCacheMap
{
	public void		clear();

	public void		put(SCDataBlock block);

	public SCDataBlock	get(String key);

	public void		purgeStale();
}
