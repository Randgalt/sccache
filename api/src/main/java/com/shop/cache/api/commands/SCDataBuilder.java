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
package com.shop.cache.api.commands;

import com.shop.util.chunked.ChunkedByteArray;
import com.shop.cache.api.server.SCConnection;
import com.shop.cache.api.server.SCServer;

/**
 * Mechanism for passing arguments to commands. The command returns the arguments it needs
 * and this builder is filled with values. After all arguments have been filled, executeCommand() is called.
 *
 * @author Jordan Zimmerman
 */
public interface SCDataBuilder
{
	/**
	 * Adds a string argument
	 *
	 * @param value argument
	 */
	public void		addNextValue(String value);

	/**
	 * Adds an object argument
	 *
	 * @param o argument
	 */
	public void		addNextObject(ChunkedByteArray o);

	/**
	 * Execute the command
	 *
	 * @param server the server object
	 * @param connection the connection/client
	 * @throws Exception any errors
	 */
	public void		executeCommand(SCServer server, SCConnection connection) throws Exception;
}
