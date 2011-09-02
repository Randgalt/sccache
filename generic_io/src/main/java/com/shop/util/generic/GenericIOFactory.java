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

import com.shop.util.SSLSocketMaker;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

/**
 * Factory for creating clients and servers
 *
 * @author Jordan Zimmerman
 */
public class GenericIOFactory
{
	/**
	 * Create a new server. You must call {@link GenericIOServer#start()} to start the server.
	 *
	 * @param listener event listener (cannot be null)
	 * @param parameters server parameters
	 * @return the new server instance
	 * @throws Exception errors
	 */
	public static<T> GenericIOServer<T>	makeServer(GenericIOServerListener<T> listener, GenericIOParameters parameters) throws Exception
	{
		ServerSocket		localServerSocket = parameters.getSSL() ? SSLSocketMaker.makeServer(parameters.getPort(), BACKLOG) : new ServerSocket(parameters.getPort(), BACKLOG);
		try
		{
			return new GenericIOServerImpl<T>(localServerSocket, listener);
		}
		catch ( Exception e )
		{
			if ( localServerSocket != null )
			{
				localServerSocket.close();
			}
			throw e;
		}
	}

	/**
	 * Create a new client connection. You should put the client into a read loop. You can use {@link GenericIOServer#runInThread(Runnable)}
	 * for this, {@link GenericIOLineProcessor} or your own mechanism.
	 * 
	 * @param address address/IP to connect to
	 * @param parameters client parameters
	 * @return the new client
	 * @throws Exception errors
	 */
	public static<T> GenericIOClient<T> makeClient(String address, GenericIOParameters parameters) throws Exception
	{
		Socket		localSocket = parameters.getSSL() ? SSLSocketMaker.make(address, parameters.getPort()) : new Socket(address, parameters.getPort());
		try
		{
			return new GenericIOClientImpl<T>(localSocket, null);
		}
		catch ( IOException e )
		{
			if ( localSocket != null )
			{
				localSocket.close();
			}
			throw e;
		}
	}

	private static final int 	BACKLOG = 256;
}
