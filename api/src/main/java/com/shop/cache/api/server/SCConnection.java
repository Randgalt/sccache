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

import com.shop.util.chunked.ChunkedByteArray;
import java.io.IOException;

/**
 * Abstraction for a client connected to the server
 *
 * @author Jordan Zimmerman
 */
public interface SCConnection
{
	/**
	 * Return true if this is a connection to the monitor server
	 *
	 * @return true/false
	 */
	public boolean		isMonitorMode();

	/**
	 * Close the connection
	 *
	 * @throws IOException errors
	 */
	public void 		close() throws IOException;

	/**
	 * Send a list of values to the client
	 *
	 * @param v values to send
	 * @throws IOException errors
	 */
	public void 		sendValue(String... v) throws IOException;

	/**
	 * Send an object to the client
	 *
	 * @param obj the object
	 * @throws IOException errors
	 */
	public void 		sendObject(ChunkedByteArray obj) throws IOException;
}
