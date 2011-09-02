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
package com.shop.cache.api.commands;

import com.shop.util.chunked.ChunkedByteArray;
import com.shop.cache.api.server.SCConnection;
import com.shop.cache.api.server.SCServer;
import com.shop.cache.api.common.SCDataSpec;
import com.shop.cache.api.common.SCGroup;
import com.shop.cache.api.common.SCGroupSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Jordan Zimmerman
 */
@SCDoc
(
	description = "Puts an object into the cache.",
	parameters =
	{
		"key",		"The key of the object to get",
		"ttl",		"Time in the future when this object expires",
		"group qty",	"Number of group specs (can be 0)",
		"group specs",	"The group specs",
		"size",		"The size of the data",
		"data",		"The object data"
	}
)
public class SCCommandPutObject implements SCCommand
{
	SCCommandPutObject()
	{
	}

	@Override
	public List<SCDataBuilderTypeAndCount> getTypesAndCounts()
	{
		return fTypesAndCounts;
	}

	@Override
	public SCDataBuilder newBuilder()
	{
		return new SCDataBuilder()
		{
			@Override
			public void addNextValue(String value)
			{
				switch ( index++ )
				{
					case 0:
					{
						key = value;
						break;
					}

					case 1:
					{
						try
						{
							ttl = Long.parseLong(value);
						}
						catch ( NumberFormatException e )
						{
							// ignore
						}
						break;
					}

					default:
					{
						if ( groups == null )
						{
							groups = new ArrayList<SCGroup>();
						}
						groups.add(new SCGroup(value));
						break;
					}
				}
			}

			@Override
			public void addNextObject(ChunkedByteArray o)
			{
				object = o;
			}

			@Override
			public void executeCommand(SCServer server, SCConnection connection) throws Exception
			{
				server.put(key, new SCDataSpec(object, ttl), makeGroupSpec(groups));
			}

			private String				key = "";
			private long				ttl = 0;
			private List<SCGroup>		groups = null;
			private ChunkedByteArray	object = null;
			private int					index = 0;
		};
	}

	@Override
	public boolean isMonitorCommand()
	{
		return true;
	}

	private SCGroupSpec makeGroupSpec(final List<SCGroup> groups)
	{
		if ( groups != null )
		{
			return new SCGroupSpec(groups);
		}
		return null;
	}

	private static final List<SCDataBuilderTypeAndCount>		fTypesAndCounts = Collections.unmodifiableList
	(
		Arrays.asList
		(
			new SCDataBuilderTypeAndCount(SCDataBuilderTypes.FIXED_SIZE_VALUE_SET, 2),
			new SCDataBuilderTypeAndCount(SCDataBuilderTypes.BOUNDED_VALUE_SET),
			new SCDataBuilderTypeAndCount(SCDataBuilderTypes.OBJECT)
		)
	);
}