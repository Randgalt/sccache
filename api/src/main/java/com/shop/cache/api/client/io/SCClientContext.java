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

import com.shop.cache.api.common.SCNotifications;
import java.net.InetSocketAddress;

/**
 * Builder interface for containing client data. NOTE: it is not necessary to set every
 * value. Implementations should provide reasonable defaults (except for the client address)
 *
 * @author Jordan Zimmerman
 */
public interface SCClientContext
{
	/**
	 * Set the address of the client
	 *
	 * @param address address (host and port)
	 * @return this
	 */
	public SCClientContext			address(InetSocketAddress address);

	/**
	 * Set a notification object
	 *
	 * @param n new notification object
	 * @return tbis
	 */
	public SCClientContext 			notificationHandler(SCNotifications n);

	/**
	 * Returns the currently set address
	 *
	 * @return address
	 */
	public InetSocketAddress		getAddress();

	/**
	 * Returns the current notification handler
	 *
	 * @return handler or null
	 */
	public SCNotifications 			getNotificationHandler();
}
