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

package com.shop.cache.imp.server;

import com.shop.cache.api.commands.SCCommand;
import com.shop.cache.api.commands.SCDataBuilder;
import com.shop.cache.api.commands.SCDataBuilderTypeAndCount;
import com.shop.cache.api.commands.SCSetOfCommands;
import com.shop.cache.api.server.SCConnection;
import com.shop.util.chunked.ChunkedByteArray;
import com.shop.util.generic.GenericIOClient;
import com.shop.util.generic.GenericIOLineProcessor;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Jordan Zimmerman
 */
class ImpSCServerConnection implements SCConnection, GenericIOLineProcessor.AcceptLine<ImpSCServerConnection>
{
	ImpSCServerConnection(ImpSCServer server, GenericIOClient<ImpSCServerConnection> client, boolean isMonitorMode)
	{
		fServer = server;
		fClient = client;
		fIsMonitorMode = isMonitorMode;
		fCurrentCommand = null;
		fTimeCreated = fLastCommandTime = System.currentTimeMillis();
	}

	String 		getCurrentCommand()
	{
		return (fCurrentCommand != null) ? fCurrentCommand : "<idle>";
	}

	long 		getLastCommandTime()
	{
		return fLastCommandTime;
	}

	long 		getCreationTime()
	{
		return fTimeCreated;
	}

	@Override
	public String toString()
	{
		return fClient.toString();
	}

	@Override
	public boolean isMonitorMode()
	{
		return fIsMonitorMode;
	}

	@Override
	public void close() throws IOException
	{
		fClient.close();
	}

	@Override
	public void sendValue(String... v) throws IOException
	{
		for ( String s : v )
		{
			fClient.send(s);
		}
	}

	@SuppressWarnings({"ConstantConditions"})
    @Override
	public void sendObject(ChunkedByteArray obj) throws IOException
	{
		int 		size = (obj != null) ? obj.size() : 0;

		fClient.send(Integer.toString(size));
		if ( size > 0 )
		{
			obj.writeTo
			(
				new OutputStream()
				{
					@Override
					public void write(int b) throws IOException
					{
						fClient.sendByte((byte)(b & 0xff));
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

	@Override
	public void line(GenericIOClient<ImpSCServerConnection> impSCServerConnectionXGenericIOClient, String line) throws Exception
	{
		fCurrentCommand = line;
		fLastCommandTime = System.currentTimeMillis();
		
		SCCommand 	command = SCSetOfCommands.get(fCurrentCommand);
		if ( fIsMonitorMode && (command != null) && !command.isMonitorCommand() )
		{
			command = null;
		}
		if ( command != null )
		{
			SCDataBuilder		builder = command.newBuilder();
			for ( SCDataBuilderTypeAndCount tc : command.getTypesAndCounts() )
			{
				switch ( tc.type )
				{
					case FIXED_SIZE_VALUE_SET:
					{
						for ( int i = 0; i < tc.count; ++i )
						{
							builder.addNextValue(fClient.readLine());
						}
						break;
					}

					case BOUNDED_VALUE_SET:
					{
						int			lineQty = sizeFromLine(fClient.readLine());
						for ( int i = 0; i < lineQty; ++i )
						{
							builder.addNextValue(fClient.readLine());
						}
						break;
					}

					case OBJECT:
					{
						int		size = sizeFromLine(fClient.readLine());
						if ( size > 0 )
						{
							ChunkedByteArray		bytes = fClient.readBytes(size);
							builder.addNextObject(bytes);
						}
						break;
					}

					case UNBOUNDED_VALUE_SET:
					{
						for(;;)
						{
							String 		nextLine = fClient.readLine();
							if ( nextLine.trim().length() == 0 )
							{
								break;
							}
							builder.addNextValue(nextLine);
						}
						break;
					}
				}
			}

			builder.executeCommand(fServer, this);
			fClient.flush();
		}

		fCurrentCommand = null;
	}

	@Override
	public void notifyException(Exception e)
	{
		fServer.notifyException(e);
	}

	private int sizeFromLine(String line)
	{
		int			size = 0;
		try
		{
			size = Integer.parseInt(line);
		}
		catch ( NumberFormatException e )
		{
			// ignore
		}

		return size;
	}

	private final ImpSCServer 								fServer;
	private final GenericIOClient<ImpSCServerConnection> 	fClient;
	private final boolean 									fIsMonitorMode;
	private final long										fTimeCreated;
	private volatile String									fCurrentCommand;
	private volatile long									fLastCommandTime;
}
