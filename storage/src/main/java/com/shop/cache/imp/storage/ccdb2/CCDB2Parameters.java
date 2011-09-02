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
package com.shop.cache.imp.storage.ccdb2;

/**
 * Parameter builder for the CCDB2 database
 *
 * @author Jordan Zimmerman
 */
public class CCDB2Parameters implements Cloneable
{
	/**
	 * CCDB2 requires a maximum age of objects. Regardless of the TTL, objects
	 * will never live longer than this maximum. The default is 12 hours.
	 *
	 * @param i new maximum
	 * @return this
	 */
	public CCDB2Parameters		maxAgeMilliseconds(int i)
	{
		fMaxAgeMilliseconds = i;
		return this;
	}

	/**
	 * Sets how many DB files to chain. You shouldn't have any reason to change the default (3).
	 *
	 * @param i new value
	 * @return this
	 */
	public CCDB2Parameters		maxInstances(int i)
	{
		fMaxInstances = i;
		return this;
	}

	/**
	 * Length for the background put queue. If greater than 0, put writes will occur in a background thread but the
	 * queue will block if it gets longer than the specified length. The default is 1000.
	 *
	 * @param i new value or 0
	 * @return this
	 */
	public CCDB2Parameters		backgroundPutLength(int i)
	{
		fBackgroundPutLength = i;
		return this;
	}

	/**
	 * The file prefix to use for files that CCDB2 creates. The default is "ccdb2".
	 *
	 * @param s new value
	 * @return this
	 */
	public CCDB2Parameters		filePrefix(String s)
	{
		fFilePrefix = s;
		return this;
	}

	/**
	 * The file extension to use for files that CCDB2 creates. The default is ".db".
	 *
	 * @param s new value
	 * @return this
	 */
	public CCDB2Parameters 		dbFileExtension(String s)
	{
		fDBFileExtension = s;
		return this;
	}

	/**
	 * The file extension to use for index files that CCDB2 creates. The default is ".idx".
	 *
	 * @param s new value
	 * @return this
	 */
	public CCDB2Parameters 		indexFileExtension(String s)
	{
		fIndexFileExtension = s;
		return this;
	}

	@Override
	public CCDB2Parameters clone()
	{
		try
		{
			return (CCDB2Parameters)super.clone();
		}
		catch ( CloneNotSupportedException e )
		{
			// will never get here
			return null;
		}
	}

	String getDBFileExtension()
	{
		return fDBFileExtension;
	}

	String getIndexFileExtension()
	{
		return fIndexFileExtension;
	}

	int		getMaxAgeMilliseconds()
	{
		return fMaxAgeMilliseconds;
	}

	int		getShuffleMilliseconds()
	{
		return fMaxAgeMilliseconds / 2;
	}

	int		getMaxInstances()
	{
		return fMaxInstances;
	}

	String	getFilePrefix()
	{
		return fFilePrefix;
	}

	int		getBackgroundPutLength()
	{
		return fBackgroundPutLength;
	}

	private int			fMaxAgeMilliseconds = 12 * 60 * 60 * 1000;	// 12 hours
	private int			fMaxInstances = 3;
	private int			fBackgroundPutLength = 1000;
	private String		fFilePrefix = "ccdb2";
	private String		fDBFileExtension = ".db";
	private String		fIndexFileExtension = ".idx";
}
