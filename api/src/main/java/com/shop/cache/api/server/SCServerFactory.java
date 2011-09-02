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
package com.shop.cache.api.server;

import com.shop.cache.api.storage.SCStorage;

/**
 * Factory for generating various server objects. 
 *
 * @author Jordan Zimmerman
 */
public interface SCServerFactory
{
	/**
	 * Allocate a new server context
	 *
	 * @return context
	 */
	public SCServerContext 	newContext();

	/**
	 * Allocate a new server. To start the server, you must dedicate a thread and call {@link SCServer#join()} 
	 *
	 * @param context the server context
	 * @param storage the storage instance (usually from CCDB2StorageFactory)
	 * @return the server
	 * @throws Exception errors
	 */
	public SCServer 		newServer(SCServerContext context, SCStorage storage) throws Exception;
}
