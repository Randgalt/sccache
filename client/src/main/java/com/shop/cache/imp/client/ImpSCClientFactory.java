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
package com.shop.cache.imp.client;

import com.shop.cache.api.client.io.SCClient;
import com.shop.cache.api.client.io.SCClientContext;
import com.shop.cache.api.client.io.SCClientFactory;
import com.shop.cache.api.client.io.SCClientManager;

/**
 * SHOP.COM's Client Factory implementation
 *
 * @author Jordan Zimmerman
 */
public class ImpSCClientFactory implements SCClientFactory
{
	@Override
	public SCClient newClient(SCClientContext context) throws Exception
	{
		return new ImpSCClient(context);
	}

	@Override
	public SCClientManager newClientManager(SCClientContext context) throws Exception
	{
		return new ImpSCClientManager(context);
	}

	@Override
	public SCClientContext newContext()
	{
		return new ImpSCClientContext();
	}
}
