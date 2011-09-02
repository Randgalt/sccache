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

import java.io.IOException;

/**
 * Utility to read lines from a client in a loop. Instances maintain an internal threaded-loop that reads a line
 * from the client and then calls the given line acceptor for processing
 *
 * @author Jordan Zimmerman
 */
public class GenericIOLineProcessor<T>
{
	/**
	 * Driver to receive lines
	 */
	public interface AcceptLine<T>
	{
		/**
		 * Called each time a line is read
		 *
		 * @param client the client
		 * @param line the line read
		 * @throws Exception exceptions - {@link #notifyException(Exception)} will be called and the client will be closed
		 */
		public void			line(GenericIOClient<T> client, String line) throws Exception;

		/**
		 * Called when there is an internal exception. Log the error. The processor will close the connection
		 *
		 * @param e the exception
		 */
		public void 		notifyException(Exception e);
	}

	/**
	 * @param client the client
	 * @param driver the driver
	 */
	public GenericIOLineProcessor(GenericIOClient<T> client, AcceptLine<T> driver)
	{
		fClient = client;
		fDriver = driver;
	}

	/**
	 * Start the processor loop. This method returns immediately. 
	 */
	public void		execute()
	{
		GenericIOServerImpl.internalRunInThread
		(
			new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						while ( !Thread.currentThread().isInterrupted() && fClient.isOpen() )
						{
							String		line = fClient.readLine();
							if ( line == null )
							{
								break;
							}

							fDriver.line(fClient, line);
						}
					}
					catch ( Exception e )
					{
						fDriver.notifyException(e);
					}
					finally
					{
						try
						{
							fClient.close();
						}
						catch ( IOException ignore )
						{
							// ignore
						}
					}
				}
			}
		);
	}

	private final GenericIOClient<T> 	fClient;
	private final AcceptLine<T> 		fDriver;
}
