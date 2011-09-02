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
package com.shop.cache.api.client.io;

import java.net.InetSocketAddress;

/**
 * Factory for generating various client objects. 
 *
 * @author Jordan Zimmerman
 */
public interface SCClientFactory
{
	/**
	 * Allocate a new client context
	 *
	 * @return the context
	 */
	public SCClientContext	newContext();

	/**
	 * Allocate a new client based on the given context. Be sure to set, at minimum, {@link SCClientContext#address(InetSocketAddress)}. A
	 * client is a single connection to a cache server.
	 *
	 * @param context the context
	 * @return the client
	 * @throws Exception errors
	 */
	public SCClient			newClient(SCClientContext context) throws Exception;

	/**
	 * Allocate a new manager based on the given context. Be sure to set, at minimum, {@link SCClientContext#address(InetSocketAddress)}. A
	 * manager manages a pool of connections to a cache server. A list of managers can be combined into a farm via {@link SCMultiManager}.
	 *
	 * @param context the context
	 * @return the client
	 * @throws Exception errors
	 */
	public SCClientManager	newClientManager(SCClientContext context) throws Exception;
}
