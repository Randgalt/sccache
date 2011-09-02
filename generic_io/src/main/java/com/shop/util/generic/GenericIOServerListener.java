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

/**
 * Callback/notifications for servers
 *
 * @author Jordan Zimmerman
 */
public interface GenericIOServerListener<T>
{
	/**
	 * Called when an internal exception has occurred. You should log a diagnostic.
	 *
	 * @param server the server
	 * @param e the exception
	 */
	public void 		notifyException(GenericIOServer<T> server, Exception e);

	/**
	 * Called when the server accepts a connection. You should put the client into a read loop. You can use {@link GenericIOServer#runInThread(Runnable)}
	 * for this, {@link GenericIOLineProcessor} or your own mechanism
	 *
	 * @param server the server
	 * @param client the new client
	 * @throws Exception errors (the server will close the connection on errors)
	 */
	public void 		notifyClientAccepted(GenericIOServer<T> server, GenericIOClient<T> client) throws Exception;

	/**
	 * Called when the server is shutting down. Prior to this method being called, all clients of
	 * the server will have been closed
	 *
	 * @param server the server
	 * @throws Exception any errors
	 */
	public void			notifyServerClosing(GenericIOServer<T> server) throws Exception;
}
