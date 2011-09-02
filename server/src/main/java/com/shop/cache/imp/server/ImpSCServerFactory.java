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
package com.shop.cache.imp.server;

import com.shop.cache.api.server.SCServerFactory;
import com.shop.cache.api.server.SCServerContext;
import com.shop.cache.api.server.SCServer;
import com.shop.cache.api.storage.SCStorage;

/**
 * SHOP.COM's Server Factory implementation
 *
 * @author Jordan Zimmerman
 */
public class ImpSCServerFactory implements SCServerFactory
{
	@Override
	public SCServerContext newContext()
	{
		return new ImpSCServerContext();
	}

	@Override
	public SCServer newServer(SCServerContext context, SCStorage database) throws Exception
	{
		ImpSCServer server = new ImpSCServer(context, database);
		database.setStorageServerDriver(server);
		return server;
	}
}
