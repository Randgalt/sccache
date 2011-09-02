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

import com.shop.cache.api.client.io.SCClientContext;
import com.shop.cache.api.common.SCNotifications;
import java.net.InetSocketAddress;

/**
 * SHOP.COM's Client Context implementation
 *
 * @author Jordan Zimmerman
 */
class ImpSCClientContext implements SCClientContext
{
	@Override
	public SCClientContext address(InetSocketAddress address)
	{
		fAddress = address;
		return this;
	}

	@Override
	public InetSocketAddress getAddress()
	{
		return fAddress;
	}

	@Override
	public SCClientContext notificationHandler(SCNotifications n)
	{
		fNotifications = n;
		return this;
	}

	@Override
	public SCNotifications getNotificationHandler()
	{
		return fNotifications;
	}

	private InetSocketAddress			fAddress = null;
	private SCNotifications				fNotifications = null;
}
