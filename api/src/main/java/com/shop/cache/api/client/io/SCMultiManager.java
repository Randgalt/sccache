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

import com.shop.cache.api.common.SCDataSpec;
import com.shop.cache.api.common.SCGroup;
import com.shop.cache.api.common.SCGroupSpec;
import com.shop.cache.api.common.SCNotifications;
import com.shop.util.chunked.ChunkedByteArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages a farm of cache servers as an integrated set. Cached objects are sent to specific
 * servers based on the hashCode of the object's key
 *
 * @author Jordan Zimmerman
 */
public class SCMultiManager implements SCManager, Iterable<SCClientManager>
{
	/**
	 * Create the manager with the given set of clients. IMPORTANT: the list should be the same size and in
	 * the same order each time. Any changes will cause all objects in the cache to become missing. 
	 *
	 * @param clientSet set of clients
	 */
	public SCMultiManager(List<SCClientManager> clientSet)
	{
		this(clientSet, null);
	}

	/**
	 * Create the manager with the given set of clients. IMPORTANT: the list should be the same size and in
	 * the same order each time. Any changes will cause all objects in the cache to become missing.
	 *
	 * @param clientSet set of clients
	 * @param hasher hasher to use to map a key to a client index. Pass null for the default hasher
	 */
	public SCMultiManager(List<SCClientManager> clientSet, SCHasher hasher)
	{
		fClientSet = Collections.unmodifiableList(new ArrayList<SCClientManager>(clientSet));
		fHasher = (hasher != null) ? hasher : new DefaultHasher();
		fIsOpen = new AtomicBoolean(true);
		fNotificationHandler = null;
	}

	@Override
	public SCNotifications getNotificationHandler()
	{
		return fNotificationHandler;
	}

	/**
	 * Set a handler to be called at interesting events
	 *
	 * @param handler new handler or null to clear
	 */
	public void 		setNotificationHandler(SCNotifications handler)
	{
		fNotificationHandler = handler;
	}

	/**
	 * Close this manager (and all sub-managers). This instance will no longer
	 * be usable after this method call
	 */
	@Override
	public void close()
	{
		if ( fIsOpen.compareAndSet(false, true) )
		{
			for ( SCClientManager manager : fClientSet )
			{
				manager.close();
			}
		}
	}

	@Override
	public Iterator<SCClientManager> iterator()
	{
		final Iterator<SCClientManager> 	localIterator = fClientSet.iterator();
		return new Iterator<SCClientManager>()
		{
			@Override
			public boolean hasNext()
			{
				return localIterator.hasNext();
			}

			@Override
			public SCClientManager next()
			{
				return localIterator.next();
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Return the TTL for the given key
	 *
	 * @param key key to check
	 * @return TTL or 0 if not found
	 * @throws Exception errors
	 */
	@Override
	public long getTTL(String key) throws Exception
	{
		checkOpen();

		SCClient			client = null;
		try
		{
			client = getClientForKey(key, ListTypes.STANDARD);
			if ( client != null )
			{
				if ( fNotificationHandler != null )
				{
					fNotificationHandler.notifyClientAccess(client.getManager().toString());
				}

				return client.getTTL(key);
			}
		}
		finally
		{
			releaseClient(client);
		}

		return 0;
	}

	@Override
	public List<String> removeGroup(SCGroup group) throws Exception
	{
		checkOpen();

		Set<String> 		keys = new HashSet<String>();
		for ( SCClientManager manager : fClientSet )
		{
			SCClient 	client = manager.getClient();
			if ( client != null )
			{
				List<String> thisKeyList = client.removeGroup(group);
				keys.addAll(thisKeyList);
			}
		}

		return new ArrayList<String>(keys);
	}

	@Override
	public List<String> listGroup(SCGroup group) throws Exception
	{
		checkOpen();

		Set<String> 		keys = new HashSet<String>();
		for ( SCClientManager manager : fClientSet )
		{
			SCClient 	client = manager.getClient();
			if ( client != null )
			{
				List<String> thisKeyList = client.listGroup(group);
				keys.addAll(thisKeyList);
			}
		}

		return new ArrayList<String>(keys);
	}

	@Override
	public List<String> dumpStats(boolean verbose) throws Exception
	{
		checkOpen();

		List<String> tab = new ArrayList<String>();
		for ( SCClientManager manager : fClientSet )
		{
			SCClient 	client = manager.getClient();
			if ( client != null )
			{
				List<String> thisTab = client.dumpStats(verbose);
				tab.addAll(thisTab);
			}
		}

		return tab;
	}

	@Override
	public List<String> stackTrace() throws Exception
	{
		checkOpen();

		List<String> tab = new ArrayList<String>();
		for ( SCClientManager manager : fClientSet )
		{
			SCClient 	client = manager.getClient();
			if ( client != null )
			{
				List<String> thisTab = client.stackTrace();
				tab.addAll(thisTab);
			}
		}

		return tab;
	}

	@Override
	public List<String> getConnectionList() throws Exception
	{
		checkOpen();

		List<String> tab = new ArrayList<String>();
		for ( SCClientManager manager : fClientSet )
		{
			SCClient 	client = manager.getClient();
			if ( client != null )
			{
				List<String> thisTab = client.getConnectionList();
				tab.addAll(thisTab);
			}
		}

		return tab;
	}

    @Override
    public List<String> regExFindKeys(String expression) throws Exception
    {
		checkOpen();

		Set<String> 		keys = new HashSet<String>();
		for ( SCClientManager manager : fClientSet )
		{
			SCClient 	client = manager.getClient();
			if ( client != null )
			{
				List<String> thisKeyList = client.regExFindKeys(expression);
				keys.addAll(thisKeyList);
			}
		}

		return new ArrayList<String>(keys);
	}

    @Override
	public List<String> regExRemove(String expression) throws Exception
	{
		checkOpen();

		Set<String> 		keys = new HashSet<String>();
		for ( SCClientManager manager : fClientSet )
		{
			SCClient 	client = manager.getClient();
			if ( client != null )
			{
				List<String> thisKeyList = client.regExRemove(expression);
				keys.addAll(thisKeyList);
			}
		}

		return new ArrayList<String>(keys);
	}

	@Override
	public void writeKeyData(String fPath) throws Exception
	{
		checkOpen();

		for ( SCClientManager manager : fClientSet )
		{
			SCClient 	client = manager.getClient();
			if ( client != null )
			{
				client.writeKeyData(fPath);
			}
		}
	}

	/**
	 * Return the data associated with the given key
	 *
	 * @param key key to check
	 * @param ignoreTTL if true, the TTL is ignored. If false and the TTL is stale, the data is not returned
	 * @return the data or null if not found
	 * @throws Exception errors
	 */
	@Override
	public ChunkedByteArray get(String key, boolean ignoreTTL) throws Exception
	{
		checkOpen();

		SCClient			client = null;
		try
		{
			client = getClientForKey(key, ListTypes.STANDARD);
			if ( client != null )
			{
				if ( fNotificationHandler != null )
				{
					fNotificationHandler.notifyClientAccess(client.getManager().toString());
				}

				return client.get(key, ignoreTTL);
			}
		}
		finally
		{
			releaseClient(client);
		}

		return null;
	}

	/**
	 * Add keyed data to the cache
	 *
	 * @param key Key
	 * @param data data and TTL
	 * @param groups groups or null for no groups
	 * @throws Exception errors
	 */
	@Override
	public void put(String key, SCDataSpec data, SCGroupSpec groups) throws Exception
	{
		checkOpen();

		internalPut(key, data, groups, ListTypes.STANDARD);
	}

	/**
	 * Same as {@link #put(String, SCDataSpec, SCGroupSpec)} - however, the data is written to
	 * 2 managed servers for safety. i.e. if the main server fails, the data is still
	 * available on the backup.
	 *
	 * @param key Key
	 * @param data data and TTL
	 * @param groups groups or null for no groups
	 * @throws Exception errors
	 */
	@Override
	public void putWithBackup(String key, SCDataSpec data, SCGroupSpec groups) throws Exception
	{
		checkOpen();

		internalPut(key, data, groups, ListTypes.STANDARD);
		internalPut(key, data, groups, ListTypes.BACKUP);
	}

	/**
	 * Remove the given key
	 *
	 * @param key key to remove
	 * @throws Exception errors
	 */
	@Override
	public void remove(String key) throws Exception
	{
		checkOpen();

		SCClient		client = null;
		try
		{
			client = getClientForKey(key, ListTypes.STANDARD);
			if ( client != null )
			{
				client.remove(key);
			}
		}
		finally
		{
			releaseClient(client);
		}
	}

	/**
	 * Common portion of the put operation
	 *
	 * @param key key
	 * @param data data
	 * @param groups groups or null
	 * @param type put type
	 * @throws Exception errors
	 */
	private void internalPut(String key, SCDataSpec data, SCGroupSpec groups, ListTypes type) throws Exception
	{
		SCClient		client = null;
		try
		{
			client = getClientForKey(key, type);
			if ( client != null )
			{
				client.put(key, data, groups);
			}
		}
		finally
		{
			releaseClient(client);
		}
	}

	/**
	 * For the given key and type, return the correct client (checking for down managers, etc.)
	 *
	 * @param key key
	 * @param type type
	 * @return client - might be null if all managers are down
	 * @throws Exception errors
	 */
	private SCClient 			getClientForKey(String key, ListTypes type) throws Exception
	{
		SCClient 				client = null;
		while ( client == null )
		{
			SCClientManager		manager = getManagerForKey(key, type);
			if ( manager == null )
			{
				break;
			}
			client = manager.getClient();
		}
		return client;
	}

	/**
	 * For the given key and type, return the correct manager (checking for down managers, etc.)
	 *
	 * @param key key
	 * @param type type
	 * @return manager - might be null if all managers are down
	 */
	private SCClientManager getManagerForKey(String key, ListTypes type)
	{
		List<SCClientManager> 	workList = new ArrayList<SCClientManager>(fClientSet);
		SCClientManager 		manager = null;
		while ( (workList.size() > 0) && (manager == null) )
		{
			int 		index = fHasher.keyToIndex(key, workList);
			manager = workList.get(index);
			workList.remove(index);

			if ( manager.serverIsDown() )
			{
				manager = null;
			}
			else if ( type == ListTypes.BACKUP )
			{
				// simulate the server being down so that the backup object is written to the correct server
				manager = null;
				type = ListTypes.STANDARD;
			}
		}

		return manager;
	}

	/**
	 * Release an in-use client
	 *
	 * @param client the client
	 */
	private void releaseClient(SCClient client)
	{
		if ( client != null )
		{
			client.getManager().releaseClient(client);
		}
	}

	/**
	 * Throw an exception if the cache has been closed
	 */
	private void checkOpen()
	{
		if ( !fIsOpen.get() )
		{
			throw new IllegalStateException("cache has been closed");
		}
	}

	private enum ListTypes
	{
		STANDARD,
		BACKUP
	}

	private final List<SCClientManager> fClientSet;
	private final SCHasher 				fHasher;
	private final AtomicBoolean			fIsOpen;
	private volatile SCNotifications 	fNotificationHandler;
}
