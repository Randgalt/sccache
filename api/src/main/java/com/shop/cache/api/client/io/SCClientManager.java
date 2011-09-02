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

/**
 * Interface for managing multiple connections to a single cache server
 *
 * @author Jordan Zimmerman
 */
public interface SCClientManager extends SCManager
{
	/**
	 * Return true if the server is down
	 *
	 * @return true/false
	 */
	public boolean 		serverIsDown();

	/**
	 * Return a client instance. {@link #releaseClient(SCClient)} must be called afterwards
	 *
	 * @return a client or null if none available
	 * @throws Exception errors
	 */
	public SCClient		getClient() throws Exception;

	/**
	 * Balances a call to {@link #getClient()} and releases the client it for other uses
	 *
	 * @param client the client
	 */
	public void			releaseClient(SCClient client);

	/**
	 * Internal threads, etc. may have generated exceptions. Return any exception and clear the internal state.
	 *
	 * @return exception or null
	 */
	public Exception	getAndClearLastException();

	/**
	 * Called to register an exception. This manager should respond by closing any clients and going into a "down" mode
	 * until new connections can be re-established.
	 *
	 * @param e exception
	 */
	public void			registerException(Exception e);
}
