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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jordan Zimmerman
 */
@SCDoc
(
	description = "Shutdown the cache server",
	parameters = {}
)
public class SCCommandShutdown implements SCCommand
{
	SCCommandShutdown()
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
			}

			@Override
			public void addNextObject(ChunkedByteArray o)
			{
			}

			@Override
			public void executeCommand(SCServer server, SCConnection connection) throws IOException
			{
				server.shutdown();
			}
		};
	}

	@Override
	public boolean isMonitorCommand()
	{
		return true;
	}

	private static final List<SCDataBuilderTypeAndCount>		fTypesAndCounts = Collections.unmodifiableList(new ArrayList<SCDataBuilderTypeAndCount>());
}