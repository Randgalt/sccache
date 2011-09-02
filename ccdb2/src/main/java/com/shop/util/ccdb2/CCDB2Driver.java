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
package com.shop.util.ccdb2;

/**
 * Interface methods for interacting with the DB<br>
 *
 * @author Jordan Zimmerman
 */
public interface CCDB2Driver
{
	/**
	 * Called when an internal exception has occurred - for debugging/logging purposes
	 *
	 * @param e the exception
	 */
	public void 		handleException(Exception e);

	/**
	 * Called when a loggable event occurs
	 *
	 * @param s the message
	 * @param e an optional exception (may be null)
	 * @param newline if true, add a newline to the message, otherwise more messages are on the way
	 */
	public void			log(String s, Throwable e, boolean newline);

	/**
	 * Return the value to use for the DB file extension (e.g. ".db")
	 *
	 * @return extension
	 */
	public String 		getDBExtension();


	/**
	 * Return the value to use for the index file extension (e.g. ".idx")
	 *
	 * @return extension
	 */
	public String 		getIndexExtension();

	/**
	 * Return true if CCDB2 should chunk objects - otherwise objects use a single byte array
	 *
	 * @return true/false
	 */
	public boolean		doChunking();

	/**
	 * Called when an object is being force deleted. If you have multiple DBs with overlapping
	 * objects, delete the object from all DBs. Otherwise just call {@link CCDB2Instance#remove(String)}.
	 *
	 * @param key the key of the object being removed
	 */
	public void 		callRemoveObject(String key);

	/**
	 * Return the file chunk size to use or 0 to use the default (16MB).
	 *
	 * @return allocation size or 0
	 */
	public long			getAllocationChunkSize();
}
