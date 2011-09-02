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
package com.shop.cache.imp.common;

import com.shop.cache.api.client.io.SCClientFactory;
import com.shop.cache.api.server.SCServerFactory;

/**
 * The main factory for using the SHOP.COM Cache System
 *
 * @author Jordan Zimmerman
 */
public class ShopComCacheFactory
{
	/**
	 * Return the SHOP.COM Server Factory singleton
	 *
	 * @return factory
	 */
	public static SCServerFactory		getServerFactory()
	{
		return ServerSingletonHolder.fServerInstance;
	}

	/**
	 * Return the SHOP.COM Client Factory singleton
	 *
	 * @return factory
	 */
	public static SCClientFactory		getClientFactory()
	{
		return ClientSingletonHolder.fClientInstance;
	}

	private ShopComCacheFactory()
	{
	}

	private static class ServerSingletonHolder
	{
		private final static SCServerFactory 		fServerInstance;
		static
		{
			SCServerFactory		instance = null;
			try
			{
				instance = (SCServerFactory)Class.forName("com.shop.cache.imp.server.ImpSCServerFactory").newInstance();
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				System.exit(0);
			}
			fServerInstance = instance;
		}
	}

	private static class ClientSingletonHolder
	{
		private final static SCClientFactory 		fClientInstance;
		static
		{
			SCClientFactory		instance = null;
			try
			{
				instance = (SCClientFactory)Class.forName("com.shop.cache.imp.client.ImpSCClientFactory").newInstance();
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				System.exit(0);
			}
			fClientInstance = instance;
		}
	}
}
