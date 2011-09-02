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
package com.shop.cache.api.common;

import com.shop.util.chunked.ChunkedByteArray;
import com.shop.cache.api.client.io.SCMultiManager;
import java.util.List;

/**
 * @author Jordan Zimmerman
 */
public interface SCClientServerCommon
{
	/**
	 * Close the connection
	 */
	public void 			close();

	/**
	 * Get an object
	 *
	 * @param key object key
	 * @param ignoreTTL if true ignore the TTL
	 * @return the object or null
	 * @throws Exception errors
	 */
	public ChunkedByteArray get(String key, boolean ignoreTTL) throws Exception;

	/**
	 * Return the object's TTL
	 *
	 * @param key key of the object
	 * @return TTL or 0
	 * @throws Exception errors
	 */
	public long 			getTTL(String key) throws Exception;

	/**
	 * put an object in the cache
	 *
	 * @param key object key
	 * @param data object data/ttl
	 * @param groups groups object belongs to or null
	 * @throws Exception errors
	 */
	public void				put(String key, SCDataSpec data, SCGroupSpec groups) throws Exception;

	/**
	 * Only available if the {@link SCMultiManager} is being used<p>
	 *
	 * Same as {@link #put(String, SCDataSpec, SCGroupSpec)} - however, the data is written to
	 * 2 managed servers for safety. i.e. if the main server fails, the data is still
	 * available on the backup.
	 *
	 * @param key Key
	 * @param data data and TTL
	 * @param groups groups or null for no groups
	 * @throws Exception errors
	 */
	public void 			putWithBackup(String key, SCDataSpec data, SCGroupSpec groups) throws Exception;

	/**
	 * Remove an object from the cache
	 *
	 * @param key object key
	 * @throws Exception errors
	 */
	public void				remove(String key) throws Exception;

	/**
	 * Return the currently set notification handler
	 *
	 * @return handler or null
	 */
	public SCNotifications 	getNotificationHandler();

	/**
	 * sccache supports associative keys via {@link SCGroup}. This method deletes all objects
	 * associated with the given group.
	 *
	 * @param group the group to delete
	 * @return list of keys deleted.
	 * @throws Exception errors
	 */
	public List<String> 	removeGroup(SCGroup group) throws Exception;

	/**
	 * sccache supports associative keys via {@link SCGroup}. This method returns all object keys
	 * associated with the given group.
	 *
	 * @param group the group to list
	 * @return list of keys
	 * @throws Exception errors
	 */
	public List<String> 		listGroup(SCGroup group) throws Exception;

	/**
	 * Returns server statistics
	 *
	 * @param verbose if true, verbose stats are returned
	 * @return list of stats
	 * @throws Exception errors
	 */
	public List<String> 		dumpStats(boolean verbose) throws Exception;

	/**
	 * Returns a stack trace from all the threads in the JVM
	 *
	 * @return list of stack traces
	 * @throws Exception errors
	 */
	public List<String> 		stackTrace() throws Exception;

	/**
	 * Returns all the current connections to the server and what command each connection is processing
	 *
	 * @return list of connections
	 * @throws Exception errors
	 */
	public List<String> 		getConnectionList() throws Exception;

	/**
	 * Deletes objects with keys that match the given regular expression
	 *
	 * @param expression regex
	 * @return list of keys deleted
	 * @throws Exception errors
	 */
	public List<String>			regExRemove(String expression) throws Exception;

	/**
	 * Searches for objects with keys that match the given regular expression
	 *
	 * @param expression regex
	 * @return list of matching keys
	 * @throws Exception errors
	 */
	public List<String>			regExFindKeys(String expression) throws Exception;

	/**
	 * The server will write a tab delimited file with information about the key index
	 *
	 * @param fPath the file to write to
	 * @throws Exception errors
	 */
	public void 				writeKeyData(String fPath) throws Exception;
}
