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

import com.shop.util.chunked.ChunkedByteArray;
import com.shop.cache.api.common.SCGroupSpec;

/**
 * POJO/builder for holding cached object data: the object, it's key, etc.
 *
 * @author Jordan Zimmerman
 */
public class SCDataBlock
{
	/**
	 * special version number that means use whatever value is stored
	 */
	public static final int			DONT_CARE_VERSION_NUMBER = Integer.MIN_VALUE;

	/**
	 * Initialize values to defaults:<br>
	 * <code>
	 * key = ""<br>
	 * canBeStoredExternally = true<br>
	 * canBeStoredInMemory = true<br>
	 * versionNumber = 1<br>
	 * TTL = current time + 12 hours<br>
	 * </code>
	 */
	public SCDataBlock()
	{
		fKey = "";
		fCanBeStoredExternally = true;
		fCanBeStoredInMemory = true;
		fVersionNumber = 1;
		fTTL = System.currentTimeMillis() + (12 * 60 * 60 * 1000);	// 12 hours
		fData = null;
		fIgnoreTTL = false;
		fReturnedTTL = -1;
		fCanBeQueued = true;
	}

	/**
	 * Initialize to defaults, but set the key to the given value
	 *
	 * @param key key
	 */
	public SCDataBlock(String key)
	{
		this();
		fKey = key;
	}

	/**
	 * Initialize to defaults, but set the key and data to the given value
	 *
	 * @param key key
	 * @param obj the data
	 */
	public SCDataBlock(String key, Object obj)
	{
		this();
		fKey = key;
		fObject = obj;
	}

	/**
	 * Getter for key
	 *
	 * @return key
	 */
	public String getKey()
	{
		return fKey;
	}

	/**
	 * Set the key
	 *
	 * @param key new key
	 * @return this
	 */
	public SCDataBlock key(String key)
	{
		fKey = key;
		return this;
	}

	/**
	 * Getter for canBeStoredExternally
	 *
	 * @return canBeStoredExternally
	 */
	public boolean getCanBeStoredExternally()
	{
		return fCanBeStoredExternally;
	}

	/**
	 * Set canBeStoredExternally
	 *
	 * @param canBeStoredExternally new value
	 * @return this
	 */
	public SCDataBlock canBeStoredExternally(boolean canBeStoredExternally)
	{
		fCanBeStoredExternally = canBeStoredExternally;
		return this;
	}

	/**
	 * Getter for canBeStoredInMemory
	 *
	 * @return canBeStoredInMemory
	 */
	public boolean getCanBeStoredInMemory()
	{
		return fCanBeStoredInMemory;
	}

	/**
	 * Set canBeStoredInMemory
	 *
	 * @param canBeStoredInMemory new value
	 * @return this
	 */
	public SCDataBlock canBeStoredInMemory(boolean canBeStoredInMemory)
	{
		fCanBeStoredInMemory = canBeStoredInMemory;
		return this;
	}

	/**
	 * Getter for versionNumber
	 *
	 * @return versionNumber
	 */
	public int getVersionNumber()
	{
		return fVersionNumber;
	}

	/**
	 * Set versionNumber
	 * @param versionNumber new value
	 * @return this
	 */
	public SCDataBlock versionNumber(int versionNumber)
	{
		fVersionNumber = versionNumber;
		return this;
	}

	/**
	 * Getter for TTL
	 *
	 * @return TTL
	 */
	public long getTTL()
	{
		return fTTL;
	}

	/**
	 * Set TTL
	 *
	 * @param ttl new TTL
	 * @return this
	 */
	public SCDataBlock ttl(long ttl)
	{
		fTTL = ttl;
		return this;
	}

	/**
	 * Getter for data
	 *
	 * @return data
	 */
	public ChunkedByteArray getData()
	{
		return fData;
	}

	/**
	 * Set data
	 *
	 * @param data new data
	 * @return this
	 */
	public SCDataBlock data(ChunkedByteArray data)
	{
		fData = data;
		if ( fData != null )
		{
			fData.lock();
		}
		return this;
	}

	/**
	 * Getter for object
	 *
	 * @return object
	 */
	public Object		getObject()
	{
		return fObject;
	}

	/**
	 * Set object
	 * @param o new object
	 * @return this
	 */
	public SCDataBlock	object(Object o)
	{
		fObject = o;
		return this;
	}

	/**
	 * Getter for ignore TTL
	 *
	 * @return ignore TTL flag
	 */
	public boolean		getIgnoreTTL()
	{
		return fIgnoreTTL;
	}

	/**
	 * Set ignoreTTL
	 * @param b new ignoreTTL
	 * @return this
	 */
	public SCDataBlock	ignoreTTL(boolean b)
	{
		fIgnoreTTL = b;
		return this;
	}

	/**
	 * Getter for Groups
	 *
	 * @return groups or null
	 */
	public SCGroupSpec	getGroups()
	{
		return fGroups;
	}

	/**
	 * Set new groups
	 * @param s new groups
	 * @return this
	 */
	public SCDataBlock	groups(SCGroupSpec s)
	{
		fGroups = s;
		return this;
	}

	/**
	 * Return the TTL returned from a get operation
	 *
	 * @return TTL or -1 if not set
	 */
	public long			getReturnedTTL()
	{
		return fReturnedTTL;
	}

	/**
	 * Sets the TTL value returned by {@link #getReturnedTTL()}
	 *
	 * @param ttl TTL to set
	 * @return this
	 */
	public SCDataBlock	returnedTTL(long ttl)
	{
		fReturnedTTL = ttl;
		return this;
	}

	/**
	 * Return true if operation can be queued (default is true)
	 *
	 * @return true/false
	 */
	public boolean		getCanBeQueued()
	{
		return fCanBeQueued;
	}

	/**
	 * change the value of the can-be-queued flag. If true (default), put operations
	 * may be queued to a background thread. If false, put operations will executed serially.
	 *
	 * @param value new value
	 * @return this
	 */
	public SCDataBlock	canBeQueued(boolean value)
	{
		fCanBeQueued = value;
		return this;
	}

	private String 				fKey;
	private boolean				fCanBeStoredExternally;
	private boolean				fCanBeStoredInMemory;
	private boolean				fCanBeQueued;
	private int					fVersionNumber;
	private long				fTTL;
	private ChunkedByteArray	fData;
	private Object				fObject;
	private boolean				fIgnoreTTL;
	private SCGroupSpec			fGroups;
	private long				fReturnedTTL;
}
