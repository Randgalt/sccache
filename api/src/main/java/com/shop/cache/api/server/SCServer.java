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

import com.shop.cache.api.common.SCClientServerCommon;

/**
 * The server API
 *
 * @author Jordan Zimmerman
 */
public interface SCServer extends SCLoggable, SCClientServerCommon
{
	/**
	 * Must be called from a dedicated thread to start the server. Will not return
	 * until the server has been shutdown
	 *
	 * @throws InterruptedException if the thread is interrupted
	 */
	public void					join() throws InterruptedException;

	/**
	 * Shutdown the server, close all connections, etc.
	 */
	public void					shutdown();

	/**
	 * If the server is down due to an error, that error is return here.
	 *
	 * @return error or null
	 */
	public String 				getErrorState();

	/**
	 * Used internally to keep track of the transaction count stat
	 */
	public void					incrementTransactionCount();
}
