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

import com.shop.cache.api.server.SCConnection;
import com.shop.cache.api.server.SCServer;
import com.shop.util.chunked.ChunkedByteArray;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Jordan Zimmerman
 */
@SCDoc
(
	description = "Delete objects from the cache that match the given regular expression",
	parameters =
	{
		"expression",		"Regex of the keys to delete"
	}
)
public class SCCommandRegexRemoveObjects implements SCCommand
{
	SCCommandRegexRemoveObjects()
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
				fKey = value;
			}

			@Override
			public void addNextObject(ChunkedByteArray o)
			{
			}

			@Override
			public void executeCommand(SCServer server, SCConnection connection) throws Exception
			{
				long				ticks = System.currentTimeMillis();
				List<String>		keys = server.regExRemove(fKey);
				long				time = System.currentTimeMillis() - ticks;
				keys.add(0, "Time: " + time + " ms");
				SCSetOfCommands.sendListEndingWithBlankLine(connection, keys);
			}

			private String			fKey = "";
		};
	}

	@Override
	public boolean isMonitorCommand()
	{
		return false;
	}

	private static final List<SCDataBuilderTypeAndCount>		fTypesAndCounts = Collections.unmodifiableList
	(
		Arrays.asList
		(
			new SCDataBuilderTypeAndCount(SCDataBuilderTypes.FIXED_SIZE_VALUE_SET, 1)
		)
	);
}