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

import com.shop.util.InterruptibleFutureTask;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages a pool of connections to a single {@link GenericIOServer} server.<br>
 *
 * @author Jordan Zimmerman
 */
public class GenericIOClientPool<T>
{
	/**
	 * Can be passed to the constructor as the value for retry_connection_ticks
	 */
	public static final int				DEFAULT_RETRY_CONNECTION_TICKS = 60 * 1000;	// 1 minute

	/**
	 * Can be passed to the constructor as the value for keep_alive_ticks
	 */
	public static final int 			DEFAULT_KEEP_ALIVE_TICKS = 5 * 60 * 1000;	// 5 minutes

	/**
	 * Create the pool
	 *
	 * @param address server's address/hostname and port
	 * @param ssl if true, use SSL connections
	 * @param retryConnectionTicks number of ticks to wait before re-trying failed connections
	 * @param keepAliveTicks ticks after witch to close old connections
	 */
	public GenericIOClientPool(InetSocketAddress address, boolean ssl, int retryConnectionTicks, int keepAliveTicks)
	{
		fAddress = address;
		fSSL = ssl;
		fRetryConnectionTicks = retryConnectionTicks;
		fKeepAliveTicks = keepAliveTicks;
		fState = new GenericIOClientPoolState<T>();
		fFreeList = new ConcurrentLinkedQueue<GenericIOClient<T>>();
		fLastKeepAliveCheck = new AtomicLong(System.currentTimeMillis());
		fListener = null;
	}

	/**
	 * Set the pool listener
	 *
	 * @param listener new listener or null
	 */
	public void		setListener(GenericIOClientPoolListener<T> listener)
	{
		fListener = listener;
	}

	/**
	 * Return the address/port for this pool
	 *
	 * @return address
	 */
	public InetSocketAddress getAddress()
	{
		return fAddress;
	}

	/**
	 * Returns true if the pool is an open state
	 *
	 * @return true/false
	 */
	public boolean 		isOpen()
	{
		return fState.isOpen();
	}

	/**
	 * Returns true if the pool is an open state and there is no current reopener
	 *
	 * @return true/false
	 */
	public boolean 		isOpenAndNoReopener()
	{
		return fState.isOpenAndNoReopener();
	}

	/**
	 * Attempt to reconnect to a server. An internal thread is started and will continue to attempt to reconnect periodically. Once
	 * reconnection is successful, {@link #get(AtomicBoolean)} will return connections. NOTE: if a reopen is currently in progress,
	 * this method does nothing.
	 *
	 * @return true if a new reopener was started - false if one already existed
	 */
	public boolean 		reopen()
	{
		InterruptibleFutureTask<GenericIOClient<T>> 		task = new InterruptibleFutureTask<GenericIOClient<T>>
		(
			new Callable<GenericIOClient<T>>()
			{
				@Override
				public GenericIOClient<T> call() throws Exception
				{
					GenericIOClient<T> client = null;
					try
					{
						while ( client == null )
						{
							Thread.sleep(fRetryConnectionTicks);
							try
							{
								GenericIOParameters 	parameters = new GenericIOParameters().ssl(fSSL).port(fAddress.getPort());
								client = GenericIOFactory.makeClient(fAddress.getHostName(), parameters);
							}
							catch ( Exception ignore )
							{
								// still down - sleep and try again
							}
						}
					}
					catch ( InterruptedException dummy )
					{
						Thread.currentThread().interrupt();
					}

					return client;
				}
			}
		);

		boolean			result;
		if ( fState.checkSetReopener(task) )
		{
			closeAllConnections();
			GenericIOServerImpl.internalRunInThread(task);
			result = true;
		}
		else
		{
			task.cancel(true);
			result = false;
		}
		return result;
	}

	/**
	 * Get a connection from the pool. IMPORTANT: each connection returned <b>must</b> released via a call to {@link #release(GenericIOClient)} or {@link #releaseAndClose(GenericIOClient)} 
	 *
	 * @param isNew if not null, will get set to true if the connection is brand new (i.e. not from the internal free list)
	 * @return the connection. Will return null if the pool is not in an open state
	 * @throws Exception any errors
	 */
	public GenericIOClient<T> get(AtomicBoolean isNew) throws Exception
	{
		AtomicReference<Boolean> 						isOpen = new AtomicReference<Boolean>(false);
		InterruptibleFutureTask<GenericIOClient<T>> 	localReopener = fState.getAndCheckDoneReopener(isOpen);
		if ( localReopener != null )
		{
			GenericIOClient<T> client = localReopener.isDone() ? localReopener.get() : null;
			if ( (client != null) && (isNew != null) )
			{
				isNew.set(true);
			}
			return client;
		}

		if ( !isOpen.get() )
		{
			return null;
		}

		GenericIOClient<T> 			client = fFreeList.poll();
		GenericIOClientImpl<T>		clientImpl = (GenericIOClientImpl<T>)client;
		if ( (client == null) || !clientImpl.isOpen() )
		{
			if ( isNew != null )
			{
				isNew.set(true);
			}

			GenericIOParameters 	parameters = new GenericIOParameters().ssl(fSSL).port(fAddress.getPort());
			client = GenericIOFactory.makeClient(fAddress.getHostName(), parameters);
		}

		closeStale();

		return client;
	}

	/**
	 * Release a connection retrieved from {@link #get(AtomicBoolean)} and add it to the free list
	 *
	 * @param client the connection (cannot be null)
	 */
	public void				release(GenericIOClient<T> client)
	{
		assert client != null;
		//noinspection ConstantConditions
		if ( client == null )
		{
			return;
		}

		GenericIOClientImpl<T>		clientImpl = (GenericIOClientImpl<T>)client;

		if ( clientImpl.isOpen() )
		{
			fFreeList.add(client);
		}
		else
		{
			try
			{
				client.close();
			}
			catch ( IOException e )
			{
				// ignore
			}
		}
	}

	/**
	 * Release a connection retrieved from {@link #get(AtomicBoolean)}. Unlike {@link #release(GenericIOClient)}, the connection
	 * is closed and not added to the free list. Use this method when there is an error with the client
	 *
	 * @param client the connection (cannot be null)
	 */
	public void				releaseAndClose(GenericIOClient<T> client)
	{
		assert client != null;
		//noinspection ConstantConditions
		if ( client == null )
		{
			return;
		}

		try
		{
			client.close();
		}
		catch ( IOException e )
		{
			// ignore
		}
	}

	/**
	 * Close all connections to the server and put this pool in a closed state
	 */
	public synchronized void				close()
	{
		InterruptibleFutureTask<GenericIOClient<T>> localReopener = fState.close();
		if ( localReopener != null )
		{
			localReopener.interruptTask();
			try
			{
				GenericIOClient<T> 	client = localReopener.get();
				if ( client != null )
				{
					try
					{
						client.close();
					}
					catch ( IOException e )
					{
						// ignore
					}
				}
			}
			catch ( InterruptedException dummy )
			{
				Thread.currentThread().interrupt();
			}
			catch ( ExecutionException ignore )
			{
				// ignore
			}
		}

		closeAllConnections();
	}

	private void closeAllConnections()
	{
		for ( GenericIOClient<T> client : fFreeList )
		{
			try
			{
				client.close();
			}
			catch ( IOException e )
			{
				// ignore
			}
		}
		fFreeList.clear();
	}

	private void closeStale()
	{
		if ( (System.currentTimeMillis() - fLastKeepAliveCheck.get()) > fKeepAliveTicks )
		{
			for ( GenericIOClient<T> client : fFreeList )
			{
				GenericIOClientImpl<T> 		castClient = (GenericIOClientImpl<T>)client;
				if ( ((System.currentTimeMillis() - castClient.getLastFlushTicks()) > fKeepAliveTicks) && ((System.currentTimeMillis() - castClient.getLastReadTicks()) > fKeepAliveTicks) )
				{
					if ( (fListener != null) && !fListener.staleClientClosing(castClient) )
					{
						try
						{
							// this will reset the last flush ticks
							castClient.flush();
						}
						catch ( IOException e )
						{
							// ignore
						}
						continue;
					}

					if ( fFreeList.remove(client) )
					{
						try
						{
							client.close();
						}
						catch ( IOException e )
						{
							// ignore
						}
					}
				}
			}
			fLastKeepAliveCheck.set(System.currentTimeMillis());
		}
	}

	private final InetSocketAddress							fAddress;
	private final boolean 									fSSL;
	private final int 										fRetryConnectionTicks;
	private final int 										fKeepAliveTicks;
	private final GenericIOClientPoolState<T> 				fState;
	private final ConcurrentLinkedQueue<GenericIOClient<T>>	fFreeList;
	private final AtomicLong 								fLastKeepAliveCheck;
	private volatile GenericIOClientPoolListener<T>			fListener;
}
