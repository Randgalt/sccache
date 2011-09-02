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
package com.shop.cache.api.storage;

import com.shop.cache.api.common.SCDataSpec;
import com.shop.cache.api.common.SCGroup;
import com.shop.cache.api.common.SCGroupSpec;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * interface for storage instances
 *
 * @author Jordan Zimmerman
 */
public interface SCStorage
{
	/**
	 * The server will register a driver before making any method calls
	 *
	 * @param driver the driver
	 */
	public void setStorageServerDriver(SCStorageServerDriver driver);

	/**
	 * Open the storage at the given path
	 *
	 * @param path path to the storage
	 * @throws IOException errors
	 */
	public void	open(File path) throws IOException;

	/**
	 * Return the object associated with the given key
	 *
	 * @param key the key
	 * @return the object or null if not found
	 * @throws IOException errors
	 */
	public SCDataSpec get(String key) throws IOException;

	/**
	 * Add an object to the storage
	 *
	 * @param key key
	 * @param data object
	 * @param groups associated groups or null
	 */
	public void put(String key, SCDataSpec data, SCGroupSpec groups);

	/**
	 * Close the storage. The storage instance will be unusable afterwards.
	 *
	 * @throws IOException errors
	 */
	public void close() throws IOException;

	/**
	 * Return the keys that match the given regular expression
	 *
	 * @param regex expression
	 * @return matching keys
	 */
	public Set<String> regexFindKeys(String regex);

	/**
	 * Remove the given object
	 *
	 * @param key key of the object
	 * @throws IOException errors
	 */
	public void remove(String key) throws IOException;

	/**
	 * sccache supports associative keys via {@link SCGroup}. This method deletes all objects
	 * associated with the given group.
	 *
	 * @param group the group to delete
	 * @return list of keys deleted.
	 * @throws IOException errors
	 */
	public List<String> removeGroup(SCGroup group) throws IOException;

	/**
	 * sccache supports associative keys via {@link SCGroup}. This method lists all keys
	 * associated with the given group.
	 *
	 * @param group the group to list
	 * @return list of keys
	 * @throws IOException errors
	 */
	public List<String> listGroup(SCGroup group) throws IOException;

	/**
	 * Returns storage statistics
	 *
	 * @param verbose if true, verbose stats are returned
	 * @return list of stats
	 * @throws IOException errors
	 */
	public List<String> dumpStats(boolean verbose) throws IOException;

	/**
	 * Write a tab delimited file with information about the key index
	 *
	 * @param f the file to write to
	 * @throws IOException errors
	 */
	public void writeKeyData(File f) throws IOException;
}
