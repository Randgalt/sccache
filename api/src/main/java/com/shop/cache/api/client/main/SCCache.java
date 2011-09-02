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
package com.shop.cache.api.client.main;

import com.shop.cache.api.client.io.SCManager;
import com.shop.cache.api.client.io.SCMultiManager;
import com.shop.cache.api.common.SCDataSpec;
import com.shop.cache.api.common.SCNotifications;
import com.shop.cache.api.common.SCGroup;
import com.shop.util.chunked.ChunkedByteArray;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;

/**
 * The main cache APIs for clients.
 *
 * @author Jordan Zimmerman
 */
public class SCCache
{
	/**
	 * The current release number
	 */
	public static final String 		VERSION_NUMBER = "0.7";

	/**
	 * Create a cache instance using the given manager
	 *
	 * @param manager the Manager or Multi-Manager to use
	 */
	public SCCache(SCManager manager)
	{
		this(manager, null, null);
	}

	/**
	 * Create a cache instance using the given manager, serializer and memory cache
	 *
	 * @param manager the Manager to use
	 * @param serializer the serializer to use or null for the default serializer
	 * @param memoryCache the memory cache to use or null for the default memory cache
	 */
	public SCCache(SCManager manager, SCSerializer serializer, SCMemoryCache memoryCache)
	{
		fManager = manager;
		fSerializer = (serializer != null) ? serializer : new DefaultSerializer();
		fMemoryCache = (memoryCache != null) ? memoryCache : new DefaultMemoryCache(manager);
		fNotificationHandler = null;
		fPutSet = new LinkedBlockingDeque<PutWrapper>();
		fPutThread = new Thread
		(
			new Runnable()
			{
				@Override
				public void run()
				{
					putLoop();
				}
			}
		);
		fPutThread.setDaemon(true);
		fPutThread.start();

		fIsOpen = new AtomicBoolean(true);
	}

	/**
	 * Close the cache. It will no longer be available after this method has been called
	 *
	 * @throws InterruptedException if there was a problem closing internal threads
	 */
	public void			close() throws InterruptedException
	{
		if ( fIsOpen.compareAndSet(false, true) )
		{
			fPutThread.interrupt();
			fPutThread.join();
			fManager.close();
		}
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
	 * Return the currently set notification handler
	 *
	 * @return handler or null
	 */
	public SCNotifications	getNotificationHandler()
	{
		return fNotificationHandler;
	}

	/**
	 * Put the object specified by the given data block into the cache
	 *
	 * @param block data. At minimum, {@link SCDataBlock#key(String)}, {@link SCDataBlock#ttl(long)} and {@link SCDataBlock#object(Object)} must be set.
	 * Ownership of the block is taken by this method.
	 */
	public void			put(SCDataBlock block)
	{
		boolean withBackup = false;
		internalPut(block, withBackup);
	}

	/**
	 * Only available if the {@link SCMultiManager} is being used<p>
	 *
	 * Same as {@link #put(SCDataBlock)} - however, the data is written to
	 * 2 managed servers for safety. i.e. if the main server fails, the data is still
	 * available on the backup.
	 *
	 * @param block data. At minimum, {@link SCDataBlock#key(String)}, {@link SCDataBlock#ttl(long)} and {@link SCDataBlock#object(Object)} must be set.
	 * Ownership of the block is taken by this method.
	 */
	public void			putWithBackup(SCDataBlock block)
	{
		boolean withBackup = true;
		internalPut(block, withBackup);
	}

	/**
	 * Get stats returned by {@link SCCache#get(SCDataBlock, AtomicReference)} 
	 */
	public enum GetTypes
	{
		/**
		 * The object was not in the cache
		 */
		MISSING,

		/**
		 * The object was retrieved from the memory cache
		 */
		FROM_MEMORY_CACHE,

		/**
		 * The object was reteieved from the external cache 
		 */
		FROM_EXTERNAL_CACHE
	}

	/**
	 * Try to retrieve an object from the cache
	 *
	 * @param block data. At minimum, {@link SCDataBlock#key(String)} must be set. You can also set {@link SCDataBlock#versionNumber(int)}, if the object's version
	 * in the cache doesn't match, it will be considered missing. You can also set the versionNumber to the special value {@link SCDataBlock#DONT_CARE_VERSION_NUMBER}
	 * and the versionNumber will be ignored.
	 * @return the object or null
	 */
	public Object		get(SCDataBlock block)
	{
		return get(block, null);
	}

	/**
	 * Try to retrieve an object from the cache
	 *
	 * @param block data. At minimum, {@link SCDataBlock#key(String)} must be set. You can also set {@link SCDataBlock#versionNumber(int)}, if the object's version
	 * in the cache doesn't match, it will be considered missing. You can also set the versionNumber to the special value {@link SCDataBlock#DONT_CARE_VERSION_NUMBER}
	 * and the versionNumber will be ignored.
	 * @param getType if not null, returns a status value
	 * @return the object or null
	 */
	public Object		get(SCDataBlock block, AtomicReference<GetTypes> getType)
	{
		checkOpen();

		long 					rightNow = System.currentTimeMillis();

		if ( getType != null )
		{
			getType.set(GetTypes.MISSING);
		}

		Object 					resultObject = null;
		if ( block.getCanBeStoredInMemory() )
		{
			SCDataBlock 					memoryBlock = fMemoryCache.get(block.getKey());
			if ( memoryBlock != null )
			{
				if ( checkIsUsable(block, memoryBlock, rightNow) )
				{
					resultObject = memoryBlock.getObject();
					if ( (resultObject != null) && (getType != null) )
					{
						getType.set(GetTypes.FROM_MEMORY_CACHE);
						block.returnedTTL(memoryBlock.getTTL());
					}
				}
			}
		}

		if ( (resultObject == null) && block.getCanBeStoredExternally() )
		{
			resultObject = requestObject(block, rightNow);
			if ( resultObject != null )
			{
				if ( getType != null )
				{
					getType.set(GetTypes.FROM_EXTERNAL_CACHE);
				}

				if ( block.getCanBeStoredInMemory() )
				{
					block.object(resultObject);
					fMemoryCache.put(block);
				}
			}
		}

		return resultObject;
	}

	/**
	 * Return the object's TTL
	 *
	 * @param key key of the object
	 * @return TTL or 0
	 * @throws Exception errors
	 */
	public long 			getTTL(String key) throws Exception
	{
		return fManager.getTTL(key);
	}

	/**
	 * Remove an object from the cache
	 *
	 * @param key object key
	 * @throws Exception errors
	 */
	public void				remove(String key) throws Exception
	{
		fManager.remove(key);
	}

	/**
	 * sccache supports associative keys via {@link SCGroup}. This method deletes all objects
	 * associated with the given group.
	 *
	 * @param group the group to delete
	 * @return list of keys deleted.
	 * @throws Exception errors
	 */
	public List<String> removeGroup(SCGroup group) throws Exception
	{
		return fManager.removeGroup(group);
	}

	/**
	 * sccache supports associative keys via {@link SCGroup}. This method returns all object keys
	 * associated with the given group.
	 *
	 * @param group the group to list
	 * @return list of keys
	 * @throws Exception errors
	 */
	public List<String> 		listGroup(SCGroup group) throws Exception
	{
		return fManager.listGroup(group);
	}

	/**
	 * Returns server statistics
	 *
	 * @param verbose if true, verbose stats are returned
	 * @return list of stats
	 * @throws Exception errors
	 */
	public List<String> 		dumpStats(boolean verbose) throws Exception
	{
		return fManager.dumpStats(verbose);
	}

	/**
	 * Returns a stack trace from all the threads in the JVM
	 *
	 * @return list of stack traces
	 * @throws Exception errors
	 */
	public List<String> 		stackTrace() throws Exception
	{
		return fManager.stackTrace();
	}

	/**
	 * Returns all the current connections to the server and what command each connection is processing
	 *
	 * @return list of connections
	 * @throws Exception errors
	 */
	public List<String> 		getConnectionList() throws Exception
	{
		return fManager.getConnectionList();
	}

	/**
	 * Deletes objects with keys that match the given regular expression
	 *
	 * @param expression regex
	 * @return list of keys deleted
	 * @throws Exception errors
	 */
	public List<String>			regExRemove(String expression) throws Exception
	{
		return fManager.regExRemove(expression);
	}

	/**
	 * The server will write a tab delimited file with information about the key index
	 *
	 * @param fPath the file to write to
	 * @throws Exception errors
	 */
	public void 				writeKeyData(String fPath) throws Exception
	{
		fManager.writeKeyData(fPath);
	}

	public void				purgeInMemoryCache()
	{
		fMemoryCache.clear();
	}

	private void internalPut(SCDataBlock block, boolean withBackup)
	{
		checkOpen();

		block.data(null);	// just in case

		if ( block.getCanBeStoredInMemory() )
		{
			fMemoryCache.put(block);
		}

		if ( block.getCanBeStoredExternally() )
		{
			PutWrapper 	wrapper = new PutWrapper(block, withBackup);
			if ( block.getCanBeQueued() )
			{
				fPutSet.offer(wrapper);
			}
			else
			{
				processPut(wrapper);
			}
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

	/**
	 * serialize and send the given block
	 *
	 * @param wrapper the block to send
	 */
	private void	processPut(PutWrapper wrapper)
	{
		try
		{
			ChunkedByteArray 		data = fSerializer.serialize(wrapper.block);
			SCDataSpec spec = new SCDataSpec(data, wrapper.block.getTTL());
			if ( wrapper.withBackup )
			{
				fManager.putWithBackup(wrapper.block.getKey(), spec, wrapper.block.getGroups());
			}
			else
			{
				fManager.put(wrapper.block.getKey(), spec, wrapper.block.getGroups());
			}
		}
		catch ( Throwable e )
		{
			handleException("Cache write exception", e);
		}
	}

	/**
	 * The background put loop
	 */
	private void	putLoop()
	{
		try
		{
			//noinspection InfiniteLoopStatement
			for(;;)
			{
				processPut(fPutSet.take());
			}
		}
		catch ( InterruptedException e )
		{
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Get an object from the mananger
	 *
	 * @param block object block
	 * @param rightNow current time (to check against the TTL)
	 * @return the object or null
	 */
	private Object requestObject(SCDataBlock block, long rightNow)
	{
		Object 		resultObject = null;
		try
		{
			ChunkedByteArray 	data = fManager.get(block.getKey(), block.getIgnoreTTL());
			SCDataBlock 		fromManagerBlock = (data != null) ? fSerializer.deserialize(data) : null;
			if ( fromManagerBlock != null )
			{
				boolean 		isUseable = checkIsUsable(block, fromManagerBlock, rightNow);
				if ( isUseable )
				{
					resultObject = fromManagerBlock.getObject();
					block.returnedTTL(fromManagerBlock.getTTL());
				}
			}
		}
		catch ( Throwable e )
		{
			e.printStackTrace();
			handleException("Cache read exception", e);
		}

		return resultObject;
	}

	/**
	 * If there is an exception handler, send the notification
	 *
	 * @param s a message
	 * @param e the exception
	 */
	private void handleException(String s, Throwable e)
	{
		if ( fNotificationHandler != null )
		{
			fNotificationHandler.notifyException(s, e);
		}
	}

	/**
	 * Determine if the given block is stale or usable
	 * 
	 * @param block block to check
	 * @param fromManagerBlock the manager's version of the block
	 * @param rightNow current time
	 * @return true/false
	 */
	private boolean checkIsUsable(SCDataBlock block, SCDataBlock fromManagerBlock, long rightNow)
	{
		if ( (block.getVersionNumber() != SCDataBlock.DONT_CARE_VERSION_NUMBER) && (block.getVersionNumber() != fromManagerBlock.getVersionNumber()) )
		{
			return false;
		}

		//noinspection RedundantIfStatement
		if ( fromManagerBlock.getTTL() <= rightNow )
		{
			return false;
		}

		return true;
	}

	private static class PutWrapper
	{
		final SCDataBlock		block;
		final boolean			withBackup;

		private PutWrapper(SCDataBlock block, boolean withBackup)
		{
			this.block = block;
			this.withBackup = withBackup;
		}
	}

	private final SCManager 							fManager;
	private final SCSerializer 							fSerializer;
	private final SCMemoryCache							fMemoryCache;
	private final LinkedBlockingDeque<PutWrapper> 		fPutSet;
	private final Thread 								fPutThread;
	private final AtomicBoolean 						fIsOpen;
	private volatile SCNotifications 					fNotificationHandler;
}
