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

import com.shop.cache.api.server.SCServerContext;
import com.shop.cache.api.server.SCServerFactory;
import com.shop.cache.api.server.SCServer;
import com.shop.cache.api.storage.SCStorage;
import com.shop.cache.imp.common.ShopComCacheFactory;
import com.shop.cache.imp.storage.ccdb2.CCDB2Parameters;
import com.shop.cache.imp.storage.ccdb2.CCDB2StorageFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of a cache server runner app. This is production quality.
 *
 * @author Jordan Zimmerman
 */
public class ShopCacheServer
{
	public static void main(String[] args)
	{
		try
		{
			Arguments		arguments = getArguments(args);
			internalMain(arguments);
		}
		catch ( Throwable e )
		{
			printHelp();
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static void internalMain(Arguments arguments) throws Exception
	{
		/**
		 * This is a server, so get the server factory and a context
		 */
		SCServerFactory 		factory = ShopComCacheFactory.getServerFactory();
		SCServerContext 		context = factory.newContext();

		/**
		 * The context is where startup parameters are set. They must be
		 * set before the server is started
		 */
		context.port(arguments.portInt);
		if ( arguments.monitorPortInt != 0 )
		{
			context.monitorPort(arguments.monitorPortInt);
		}
		if ( arguments.logPath != null )
		{
			context.logPath(new File(getFileFromPath(arguments.logPath), "log" + System.currentTimeMillis() + ".txt"));
		}

		/**
		 * This server will overflow objects to disk. It uses the included
		 * CCDB2 package to do this
		 */
		SCStorage 				db = CCDB2StorageFactory.create(new CCDB2Parameters());
		db.open(getFileFromPath(arguments.dbPath));

		/**
		 * Everything's set up, we can now create the server
		 */
		final SCServer 			server = factory.newServer(context, db);

		/**
		 * Set up a hook to handle CTRL-C shutdown requests
		 */
		Thread 					hook = new Thread
		(
			new Runnable()
			{
				@Override
				public void run()
				{
					System.out.println("Shutting down...");
					server.shutdown();
				}
			}
		);
		hook.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(hook);

		/**
		 * sleep until the server process is done
		 */
		server.join();
		System.out.println("Done.");
	}

	private static void	printHelp()
	{
		System.err.println("ShopCacheServer -port -path <SSS> <NNN> -monitorport <NNN> -log <SSS>");
		System.err.println("-port - Required. The port for the server to listen on.");
		System.err.println("-path - Required. The directory path to store cache DB files.");
		System.err.println("-monitorport - Optional. A separate port for monitoring. Command set is limited to monitoring commands.");
		System.err.println("-log - Optional. The directory path to write log files.");
		System.err.println();
	}

	private static File getFileFromPath(String path)
	{
		File 	f = new File(path);
		if ( !f.exists() && !f.mkdirs() )
		{
			throw new IllegalArgumentException("Could not make directory: " + f.getPath());
		}
		return f;
	}

	private static class Arguments
	{
		final String	port;
		final String	monitorPort;
		final String	dbPath;
		final String	logPath;
		final int		portInt;
		final int		monitorPortInt;

		private Arguments(Map<String, String> args)
		{
			port = args.get("port");
			monitorPort = args.get("monitorport");
			dbPath = args.get("path");
			logPath = args.get("log");

			try
			{
				portInt = Integer.parseInt(port);
			}
			catch ( NumberFormatException e )
			{
				throw new IllegalArgumentException("bad port: " + port);
			}

			try
			{
				monitorPortInt = (monitorPort != null) ? Integer.parseInt(monitorPort) : 0;
			}
			catch ( NumberFormatException e )
			{
				throw new IllegalArgumentException("bad monitor port: " + monitorPort);
			}
		}
	}

	private static Arguments getArguments(String[] args)
	{
		Map<String, String>		arguments = new HashMap<String, String>();
		
		String					option = null;
		for ( String a : args )
		{
			if ( a.startsWith("-") )
			{
				if ( option != null )
				{
					throw new UnsupportedOperationException("Unexpected option: " + a);
				}
				option = a.substring(1);
			}
			else
			{
				if ( option == null )
				{
					throw new UnsupportedOperationException("Unexpected value: " + a);
				}
				arguments.put(option.toLowerCase(), a);
				option = null;
			}
		}

		return new Arguments(arguments);
	}

	private ShopCacheServer()
	{
	}
}
