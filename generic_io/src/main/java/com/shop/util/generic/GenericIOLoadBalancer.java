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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages connections to a farm of servers (that implement the {@link GenericIOClient}/{@link GenericIOServer} protocol).<br>
 *
 * The boilerplate code for using this class is:
<code><pre>
farm = new GenericIOFarm&lt;MyClass&gt;(addresses, ssl);

// ...

try
{
	for ( GenericIOFarm&lt;MyClass&gt; connection : farm )
	{
		try
		{
 			// do work with the connection

 			// if connection is successful, break out of the loop
		}
		catch ( Exception e )
		{
 			// let the farm know that there was an error
			farm.setException(e);
		}
	}
}
finally
{
 	// must be in a finally block so as not to leak any connections
	farm.checkException();		// can throw an exception
}

</pre></code>
 * @author Jordan Zimmerman
 */
public class GenericIOLoadBalancer<T> implements Iterable<GenericIOClient<T>>
{
	/**
	 * @param addresses list of address to balance over
	 * @param ssl if true, connections are SSL
	 */
	public GenericIOLoadBalancer(List<InetSocketAddress> addresses, boolean ssl)
	{
		fIsOpen = true;

		List<GenericIOClientPool<T>>		workList = new ArrayList<GenericIOClientPool<T>>();
		for ( InetSocketAddress address : addresses )
		{
			workList.add(new GenericIOClientPool<T>(address, ssl, GenericIOClientPool.DEFAULT_RETRY_CONNECTION_TICKS, GenericIOClientPool.DEFAULT_KEEP_ALIVE_TICKS));
		}

		fPools = Collections.unmodifiableList(workList);
	}

	/**
	 * Iterate over the farm. Using an iterator allows a failover connection to succeed if the original connection fails
	 *
	 * @return iterator
	 */
	@Override
	public Iterator<GenericIOClient<T>> iterator()
	{
		final ThreadData 		tData = fThreadIteratorData.get();
		tData.exception = null;
		tData.thisClient = null;
		tData.thisPool = null;
		if ( tData.localPools.size() > 0 )
		{
			tData.localPools.add(tData.localPools.remove(0));	// this acheives round robin
		}

		final Iterator<GenericIOClientPool<T>> 	localIterator = tData.localPools.iterator();
		return new Iterator<GenericIOClient<T>>()
		{
			@Override
			public boolean hasNext()
			{
				releaseTData(tData);

				while ( (tData.thisClient == null) && localIterator.hasNext() )
				{
					GenericIOClientPool<T> 		thisPool = localIterator.next();
					if ( thisPool.isOpenAndNoReopener() )
					{
						try
						{
							tData.thisClient = thisPool.get(null);
							tData.thisPool = thisPool;
						}
						catch ( Exception e )
						{
							tData.exception = e;
							thisPool.reopen();
						}
					}
				}

				if ( tData.thisClient != null )
				{
					tData.exception = null;
				}

				return (tData.thisClient != null);
			}

			@Override
			public GenericIOClient<T> next()
			{
				return tData.thisClient;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Set the pool listener
	 *
	 * @param listener new listener or null
	 */
	public void		setListener(GenericIOClientPoolListener<T> listener)
	{
		for ( GenericIOClientPool<T> pool : fPools )
		{
			pool.setListener(listener);
		}
	}

	/**
	 * Must be called in a finally block to end iteration
	 *
	 * @throws Exception if there was an error
	 */
	public void 	checkException() throws Exception
	{
		ThreadData 		tData = fThreadIteratorData.get();
		releaseTData(tData);
		if ( tData.exception != null )
		{
			throw tData.exception;
		}
	}

	/**
	 * Set an exception for the current iterator
	 *
	 * @param e exception
	 */
	public void 	setException(Exception e)
	{
		ThreadData tData = fThreadIteratorData.get();
		tData.exception = e;
	}

	/**
	 * Close the farm
	 */
	public synchronized void		close()
	{
		if ( fIsOpen )
		{
			fIsOpen = false;

			for ( GenericIOClientPool<T> pool : fPools )
			{
				pool.close();
			}
		}
	}

	/**
	 * Returns a list of servers that are currently down
	 *
	 * @return list (zero length if no servers are down)
	 */
	public List<InetSocketAddress> 	getDownServers()
	{
		List<InetSocketAddress>		down = new ArrayList<InetSocketAddress>();

		for ( GenericIOClientPool<T> pool : fPools )
		{
			if ( !pool.isOpenAndNoReopener() )
			{
				down.add(pool.getAddress());
			}
		}

		return down;
	}

	private void releaseTData(ThreadData tData)
	{
		if ( tData.thisClient != null )
		{
			if ( tData.exception != null )
			{
				tData.thisPool.releaseAndClose(tData.thisClient);
				tData.thisPool.reopen();
			}
			else
			{
				tData.thisPool.release(tData.thisClient);
			}
			tData.thisClient = null;
			tData.thisPool = null;
		}
	}

	private class ThreadData
	{
		final List<GenericIOClientPool<T>>		localPools = new LinkedList<GenericIOClientPool<T>>(fPools);
		Exception 								exception = null;
		GenericIOClient<T> 						thisClient = null;
		GenericIOClientPool<T>					thisPool = null;
	}

	private final ThreadLocal<ThreadData> 	fThreadIteratorData = new ThreadLocal<ThreadData>()
	{
		@Override
		protected ThreadData initialValue()
		{
			return new ThreadData();
		}
	};

	private boolean									fIsOpen;
	private final List<GenericIOClientPool<T>> 		fPools;
}
