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

import com.shop.cache.api.common.SCClientServerCommon;

/**
 * Manages sending commands to a cache server instance
 *
 * @author Jordan Zimmerman
 */
public interface SCClient extends SCClientServerCommon
{
	/**
	 * Send the hello command
	 *
	 * @throws Exception errors
	 */
	public void				hello() throws Exception;

	/**
	 * Send the goodbye command - the client will be disconnected afterwards
	 *
	 * @throws Exception errors
	 */
	public void				goodbye() throws Exception;

	/**
	 * Causes the remote server to shut down
	 *
	 * @throws Exception errors
	 */
	public void				stopServer() throws Exception;

	/**
	 * Tell the server to write a file of key/object information to the given file path
	 *
	 * @param remoteFilename where to write the file (locally to the server)
	 * @throws Exception any errors
	 */
	public void 			keyDump(String remoteFilename) throws Exception;

	/**
	 * If this client came from a manager, it is returned here
	 *
	 * @return the client's manager or null
	 */
	public SCClientManager	getManager();
}
