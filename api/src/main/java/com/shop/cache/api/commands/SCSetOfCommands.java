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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains all the available server commands
 *
 * @author Jordan Zimmerman
 */
public class SCSetOfCommands
{
	/**
	 * Return the command handler registered for the given name
	 *
	 * @param name command name
	 * @return handler or null
	 */
	public static SCCommand			get(String name)
	{
		return fCommandMap.get(name.toLowerCase());
	}

	/**
	 * Return all the registered command names
	 *
	 * @return command names
	 */
	public static Set<String>		getCommandNames()
	{
		return fCommandMap.keySet();
	}

	/**
	 * Given a command handler class, return the command's name
	 *
	 * @param commandClass class
	 * @return name
	 */
	public static String			getCommandName(Class<? extends SCCommand> commandClass)
	{
		return fReverseCommandMap.get(commandClass);
	}

	static void		sendListEndingWithBlankLine(SCConnection connection, List<String> tab) throws Exception
	{
		for ( int i = 0; i < tab.size(); ++i )
		{
			if ( tab.get(i).length() == 0 )
			{
				tab.set(i, " ");
			}
		}
		tab.add("");	// signal the end
		connection.sendValue(tab.toArray(new String[tab.size()]));
	}

	private static final Map<String, SCCommand>		fCommandMap;
	static
	{
		Map<String, SCCommand>		work = new HashMap<String, SCCommand>();
		work.put("bye", new SCCommandCloseConnection());
		work.put("hello", new SCCommandHello());
		work.put("removegroup", new SCCommandRemoveGroup());
		work.put("listgroup", new SCCommandListGroup());
		work.put("remove", new SCCommandRemoveObject());
		work.put("?", new SCCommandHelp());
		work.put("help", new SCCommandHelp());
		work.put("put", new SCCommandPutObject());
		work.put("get", new SCCommandGetObject());
		work.put("iget", new SCCommandGetObjectIgnoreTTL());
		work.put("shutdownserver", new SCCommandShutdown());
		work.put("get / http/1.1", new SCCommandHTTPPing());
		work.put("stack", new SCCommandStack());
		work.put("listclients", new SCCommandListClients());
		work.put("rdelete", new SCCommandRegexRemoveObjects());
		work.put("dump", new SCCommandDumpStats());
		work.put("sdump", new SCCommandDumpShortStats());
		work.put("keydump", new SCCommandKeyDump());
		work.put("getttl", new SCCommandGetObjectTTL());

		fCommandMap = Collections.unmodifiableMap(work);
	}

	private static final Map<Class<? extends SCCommand>, String>		fReverseCommandMap;
	static
	{
		Map<Class<? extends SCCommand>, String>		work = new HashMap<Class<? extends SCCommand>, String>();
		for ( String name : fCommandMap.keySet() )
		{
			SCCommand 		command = fCommandMap.get(name);
			work.put(command.getClass(), name);
		}

		fReverseCommandMap = Collections.unmodifiableMap(work);
	}
}
