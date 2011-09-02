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

/**
 * Abstraction for a group ID
 *
 * @author Jordan Zimmerman
 */
public class SCGroup
{
	public final long			value;

	public SCGroup(long value)
	{
		this.value = value;
	}

	public SCGroup(String value)
	{
		long		l = 0;
		try
		{
			l = Long.parseLong(value);
		}
		catch ( NumberFormatException e )
		{
			// ignore
		}
		this.value = l;
	}

	@Override
	public String toString()
	{
		return Long.toString(value);
	}
}
