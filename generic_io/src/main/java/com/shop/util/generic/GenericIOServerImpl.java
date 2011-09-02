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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Internal implementation of the server
 *
 * @author Jordan Zimmerman
 */
class GenericIOServerImpl<T> implements GenericIOServer<T>
{
	GenericIOServerImpl(ServerSocket socket, GenericIOServerListener<T> listener)
	{
		fSocket = socket;
		fListener = listener;
		fClients = Collections.newSetFromMap(new ConcurrentHashMap<GenericIOClientImpl<T>, Boolean>());
		fUserValue = new AtomicReference<T>(null);
		fIsOpen = new AtomicBoolean(false);
		fThread = new Thread
		(
			new Runnable()
			{
				@Override
				public void run()
				{
					runLoop();
				}
			}
		);
	}

	@Override
	public void start() throws IOException
	{
		if ( !fIsOpen.compareAndSet(false, true) )
		{
			throw new UnsupportedOperationException();
		}
		fIsOpen.set(true);
		fSocket.setSoTimeout(1000);
		fThread.start();
	}

	@Override
	public List<Exception> close()
	{
		List<Exception>		exceptions = new ArrayList<Exception>();
		if ( fIsOpen.compareAndSet(true, false) )
		{
			fThread.interrupt();
			try
			{
				fThread.join();
			}
			catch ( InterruptedException e )
			{
				// ignore
			}

			for ( GenericIOClientImpl<T> client : fClients )
			{
				try
				{
					client.close();
				}
				catch ( IOException e )
				{
					exceptions.add(e);
				}
			}
		}

		try
		{
			fListener.notifyServerClosing(this);
		}
		catch ( Exception e )
		{
			exceptions.add(e);
		}

		return exceptions;
	}

	@Override
	public void setUserValue(T value)
	{
		fUserValue.set(value);
	}

	@Override
	public T getUserValue()
	{
		return fUserValue.get();
	}

	@Override
	public List<GenericIOClient<T>> getClients()
	{
		return new ArrayList<GenericIOClient<T>>(fClients);
	}

	@Override
	public int getPort()
	{
		return fSocket.getLocalPort();
	}

	@Override
	public void 	runInThread(Runnable r)
	{
		internalRunInThread(r);
	}

	static void internalRunInThread(Runnable r)
	{
		fThreadPool.execute(r);
	}

	void		removeClient(GenericIOClientImpl<T> client)
	{
		fClients.remove(client);
	}

	private void runLoop()
	{
		while ( !Thread.currentThread().isInterrupted() )
		{
			try
			{
				Socket 					s = fSocket.accept();
				try
				{
					GenericIOClientImpl<T> 	client = new GenericIOClientImpl<T>(s, this);
					fListener.notifyClientAccepted(this, client);
					fClients.add(client);
				}
				catch ( IOException e )
				{
					if ( s != null )
					{
						s.close();
					}
					throw e;
				}
			}
			catch ( SocketTimeoutException ignore )
			{
				// ignore
			}
			catch ( Exception e )
			{
				fListener.notifyException(this, e);
			}
		}
	}

	private static final ExecutorService fThreadPool = Executors.newCachedThreadPool
	(
		new ThreadFactory()
		{
			@Override
			public Thread newThread(Runnable r)
			{
				Thread		thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			}
		}
	);

	private final ServerSocket					fSocket;
	private final GenericIOServerListener<T> 	fListener;
	private final Thread						fThread;
	private final Set<GenericIOClientImpl<T>>	fClients;
	private final AtomicReference<T> 			fUserValue;
	private final AtomicBoolean 				fIsOpen;
}
