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

package com.shop.util.generic;

import java.util.List;
import java.io.IOException;

/**
 * Server abstraction. Use {@link GenericIOFactory} to allocate a new server. {@link #start()} must then be called 
 *
 * @author Jordan Zimmerman
 */
public interface GenericIOServer<T>
{
	/**
	 * Start the server
	 *
	 * @throws IOException errors
	 */
	public void						start() throws IOException;

	/**
	 * Associate a custom value with this server. Use {@link #getUserValue()} to retrieve the value
	 *
	 * @param value the value
	 */
	public void 					setUserValue(T value);

	/**
	 * Returns the value set via {@link #setUserValue(Object)}
	 *
	 * @return value
	 */
	public T 						getUserValue();

	/**
	 * Returns a list of all currently connected clients
	 *
	 * @return client list (0 length if no clients)
	 */
	public List<GenericIOClient<T>>	getClients();

	/**
	 * Return the port the server is listening on
	 *
	 * @return port
	 */
	public int						getPort();

	/**
	 * Shutdown/close the server (closing all connections, etc.)
	 *
	 * @return any exceptions that occured during the close (0 length if no errors)
	 */
	public List<Exception>			close();

	/**
	 * Utility to run a process using the servers thread pool
	 *
	 * @param r process to run
	 */
	public void 					runInThread(Runnable r);
}
