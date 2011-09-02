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
package com.shop.cache.imp.client;

import com.shop.cache.api.client.io.SCClient;
import com.shop.cache.api.client.io.SCClientContext;
import com.shop.cache.api.client.io.SCClientManager;
import com.shop.cache.api.commands.*;
import com.shop.cache.api.common.SCDataSpec;
import com.shop.cache.api.common.SCGroup;
import com.shop.cache.api.common.SCGroupSpec;
import com.shop.cache.api.common.SCNotifications;
import com.shop.util.chunked.ChunkedByteArray;
import com.shop.util.generic.GenericIOClient;
import com.shop.util.generic.GenericIOFactory;
import com.shop.util.generic.GenericIOParameters;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SHOP.COM's Client implementation
 *
 * @author Jordan Zimmerman
 */
class ImpSCClient implements SCClient
{
	ImpSCClient(SCClientContext context) throws IOException
	{
		this(null, null, context);
	}

	@Override
	public SCNotifications getNotificationHandler()
	{
		return fContext.getNotificationHandler();
	}

	@Override
	public List<String> removeGroup(SCGroup group) throws Exception
	{
		return standardCommandUntilBlankLine(SCCommandRemoveGroup.class, group.toString());
	}

	@Override
	public List<String> listGroup(SCGroup group) throws Exception
	{
		return standardCommandUntilBlankLine(SCCommandListGroup.class, group.toString());
	}

	@Override
	public List<String> dumpStats(boolean verbose) throws Exception
	{
		return standardCommandUntilBlankLine(verbose ? SCCommandDumpStats.class : SCCommandDumpShortStats.class, null);
	}

	@Override
	public List<String> stackTrace() throws Exception
	{
		return standardCommandUntilBlankLine(SCCommandStack.class, null);
	}

	@Override
	public List<String> getConnectionList() throws Exception
	{
		return standardCommandUntilBlankLine(SCCommandListClients.class, null);
	}

    @Override
    public List<String> regExFindKeys(String expression) throws Exception
    {
        return standardCommandUntilBlankLine(SCCommandRegexRemoveObjects.class, expression);
    }

    @Override
	public List<String> regExRemove(String expression) throws Exception
	{
		return standardCommandUntilBlankLine(SCCommandRegexRemoveObjects.class, expression);
	}

	@Override
	public void writeKeyData(String fPath) throws Exception
	{
		try
		{
			fClient.send(SCSetOfCommands.getCommandName(SCCommandKeyDump.class));
			fClient.send(fPath);
			fClient.flush();
		}
		catch ( Exception e )
		{
			if ( fManager != null )
			{
				fManager.registerException(e);
			}
			throw e;
		}
	}

	@Override
	public long getTTL(String key) throws Exception
	{
		key = filterKey(key);

		try
		{
			fClient.send(SCSetOfCommands.getCommandName(SCCommandGetObjectTTL.class));
			fClient.send(key);
			fClient.flush();
			String		result = fClient.readLine();

			return safeParseLong(result);
		}
		catch ( Exception e )
		{
			if ( fManager != null )
			{
				fManager.registerException(e);
			}
			throw e;
		}
	}

	@Override
	public void close()
	{
		try
		{
			fClient.close();
		}
		catch ( IOException e )
		{
			// ignore
		}
	}

	@Override
	public void hello() throws Exception
	{
		try
		{
			fClient.send(SCSetOfCommands.getCommandName(SCCommandHello.class));
			fClient.flush();
			fClient.readLine();	// hello responds with one line
		}
		catch ( Exception e )
		{
			if ( fManager != null )
			{
				fManager.registerException(e);
			}
			throw e;
		}
	}

	@Override
	public void goodbye() throws IOException
	{
		simpleCommand(SCCommandCloseConnection.class);
	}

	@Override
	public void stopServer() throws IOException
	{
		simpleCommand(SCCommandShutdown.class);
	}

	@Override
	public void keyDump(String remoteFilename) throws IOException
	{
		try
		{
			fClient.send(SCSetOfCommands.getCommandName(SCCommandKeyDump.class));
			fClient.send(remoteFilename);
			fClient.flush();
		}
		catch ( IOException e )
		{
			if ( fManager != null )
			{
				fManager.registerException(e);
			}
			throw e;
		}
	}

	@Override
	public SCClientManager getManager()
	{
		return fManager;
	}

	@Override
	public ChunkedByteArray get(String key, boolean ignoreTTL) throws Exception
	{
		key = filterKey(key);

		try
		{
			ChunkedByteArray	bytes = null;
			
			String 				commandName = SCSetOfCommands.getCommandName(ignoreTTL ? SCCommandGetObjectIgnoreTTL.class : SCCommandGetObject.class);
			fClient.send(commandName);
			fClient.send(key);
			fClient.flush();

			int			size = safeParseInt(fClient.readLine());
			if ( size > 0 )
			{
				bytes = fClient.readBytes(size);
			}

			if ( (bytes == null) || (bytes.size() == 0) )
			{
				return null;
			}

			return bytes;
		}
		catch ( Exception e )
		{
			if ( fManager != null )
			{
				fManager.registerException(e);
			}
			throw new IOException(e);
		}
	}

	@Override
	public void putWithBackup(String key, SCDataSpec data, SCGroupSpec groups) throws Exception
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void put(String key, SCDataSpec spec, SCGroupSpec groups) throws Exception
	{
		key = filterKey(key);

		assert spec.data.size() > 0;
		if ( spec.data.size() == 0 )
		{
			return;
		}

		try
		{
			fClient.send(SCSetOfCommands.getCommandName(SCCommandPutObject.class));
			fClient.send(key);
			fClient.send(Long.toString(spec.ttl));
			if ( groups != null )
			{
				fClient.send(Integer.toString(groups.size()));
				for ( SCGroup g : groups )
				{
					fClient.send(g.toString());
				}
			}
			else
			{
				fClient.send("0");
			}

			writeObject(spec);

			fClient.flush();
		}
		catch ( IOException e )
		{
			if ( fManager != null )
			{
				fManager.registerException(e);
			}
			throw e;
		}
	}

	@Override
	public void remove(String key) throws Exception
	{
		key = filterKey(key);

		try
		{
			fClient.send(SCSetOfCommands.getCommandName(SCCommandRemoveObject.class));
			fClient.send(key);
			fClient.flush();
		}
		catch ( IOException e )
		{
			if ( fManager != null )
			{
				fManager.registerException(e);
			}
			throw e;
		}
	}

	ImpSCClient(SCClientManager manager, GenericIOClient<ImpSCClient> client, SCClientContext context) throws IOException
	{
		fManager = manager;
		fContext = (context != null) ? context : (((client != null) && (client.getUserValue() != null)) ? client.getUserValue().fContext : null);

		if ( client == null )
		{
			try
			{
				GenericIOParameters 	parameters = new GenericIOParameters().port(fContext.getAddress().getPort()).ssl(false);
				client = GenericIOFactory.makeClient(fContext.getAddress().getHostName(), parameters);
				client.setUserValue(this);
			}
			catch ( Exception e )
			{
				if ( fManager != null )
				{
					fManager.registerException(e);
				}
				throw new IOException(e);
			}
		}

		fClient = client;
	}

	GenericIOClient<ImpSCClient>	getClient()
	{
		return fClient;
	}

	private List<String> standardCommandUntilBlankLine(Class<? extends SCCommand> commandClass, String argument) throws Exception
	{
		String 				commandName = SCSetOfCommands.getCommandName(commandClass);
		List<String>		tab = new ArrayList<String>();
		try
		{
			fClient.send(commandName);
			if ( argument != null )
			{
				fClient.send(argument);
			}
			fClient.flush();

			for(;;)
			{
				String	line = fClient.readLine();
				if ( line == null )
				{
					throw new EOFException();
				}
				if ( line.length() == 0 )
				{
					break;
				}
				tab.add(line);
			}
		}
		catch ( Exception e )
		{
			if ( fManager != null )
			{
				fManager.registerException(e);
			}
			throw e;
		}
		return tab;
	}

	private static int safeParseInt(String s)
	{
		int			i = 0;
		try
		{
			i = Integer.parseInt(s);
		}
		catch ( NumberFormatException e )
		{
			// ignore
		}
		return i;
	}

	private static long safeParseLong(String s)
	{
		long			i = 0;
		try
		{
			i = Long.parseLong(s);
		}
		catch ( NumberFormatException e )
		{
			// ignore
		}
		return i;
	}

	private void		writeObject(SCDataSpec spec) throws IOException
	{
		if ( (spec == null) || (spec.data == null) )
		{
			fClient.send("0");
		}
		else
		{
			int 			size = spec.data.size();

			fClient.send(Integer.toString(size));
			spec.data.writeTo
			(
				new OutputStream()
				{
					@Override
					public void write(int i) throws IOException
					{
						byte		b = (byte)(i & 0xff);
						byte[]		bytes = {b};
						fClient.sendBytes(bytes, 0, 1);
					}

					@Override
					public void write(byte[] b) throws IOException
					{
						fClient.sendBytes(b, 0, b.length);
					}

					@Override
					public void write(byte[] b, int off, int len) throws IOException
					{
						fClient.sendBytes(b, off, len);
					}
				}
			);
		}
	}

	private void simpleCommand(Class<? extends SCCommand> commandClass) throws IOException
	{
		try
		{
			fClient.send(SCSetOfCommands.getCommandName(commandClass));
			fClient.flush();
		}
		catch ( IOException e )
		{
			if ( fManager != null )
			{
				fManager.registerException(e);
			}
			throw e;
		}
	}

	private static String	filterKey(String key)
	{
		StringBuilder		newKey = new StringBuilder(key.length());
		for ( int i = 0; i < key.length(); ++i )
		{
			char		c = key.charAt(i);
			switch ( c )
			{
				case '\n':
				case '\r':
				{
					newKey.append('\u0001');
					break;
				}

				default:
				{
					newKey.append(c);
					break;
				}
			}
		}
		return newKey.toString();
	}

	private final GenericIOClient<ImpSCClient> 		fClient;
	private final SCClientManager 					fManager;
	private final SCClientContext 					fContext;
}