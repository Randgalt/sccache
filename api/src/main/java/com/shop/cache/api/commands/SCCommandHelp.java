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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jordan Zimmerman
 */
@SCDoc
(
	description = "Displays this help",
	parameters = {}
)
public class SCCommandHelp implements SCCommand
{
	SCCommandHelp()
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
				List<String>		tab = new ArrayList<String>();
				output_command_help(tab, connection.isMonitorMode());
				connection.sendValue(tab.toArray(new String[tab.size()]));
			}
		};
	}

	@Override
	public boolean isMonitorCommand()
	{
		return true;
	}

	private void		output_command_help(List<String> tab, boolean monitorMode)
	{
		tab.add("Commands: ");
		tab.add("=============================");

		for ( String name : SCSetOfCommands.getCommandNames() )
		{
			SCCommand 		command = SCSetOfCommands.get(name);
			if ( monitorMode && !command.isMonitorCommand() )
			{
				continue;
			}

			SCDoc 		doc = command.getClass().getAnnotation(SCDoc.class);
			if ( doc != null )
			{
				tab.add(name);
				tab.add(doc.description());

				String[] 		parameters = doc.parameters();
				if ( parameters.length > 0 )
				{
					tab.add("\tParameters:");
					for ( int i = 0; i < parameters.length; i += 2 )
					{
						tab.add("\t\t" + parameters[i] + "\t" + parameters[i + 1]);
					}
				}
				tab.add("");
			}
		}
	}

	private static final List<SCDataBuilderTypeAndCount>		fTypesAndCounts = Collections.unmodifiableList(new ArrayList<SCDataBuilderTypeAndCount>());
}