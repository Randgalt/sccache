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

import com.shop.cache.api.common.SCDataSpec;
import com.shop.cache.api.common.SCGroup;
import com.shop.cache.api.common.SCGroupSpec;
import com.shop.cache.api.common.SCNotifications;
import com.shop.cache.api.server.SCServer;
import com.shop.cache.api.server.SCServerContext;
import com.shop.cache.api.storage.SCStorage;
import com.shop.cache.api.storage.SCStorageServerDriver;
import com.shop.cache.imp.common.ImpSCUtils;
import com.shop.util.chunked.ChunkedByteArray;
import com.shop.util.generic.GenericIOClient;
import com.shop.util.generic.GenericIOClientPoolListener;
import com.shop.util.generic.GenericIOLineProcessor;
import com.shop.util.generic.GenericIOFactory;
import com.shop.util.generic.GenericIOServer;
import com.shop.util.generic.GenericIOServerListener;
import com.shop.util.generic.GenericIOParameters;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * SHOP.COM's Server implementation
 *
 * @author Jordan Zimmerman
 */
class ImpSCServer implements SCServer, SCStorageServerDriver
{
	ImpSCServer(SCServerContext context, SCStorage database) throws Exception
	{
		fDatabase = database;

		fAbnormalCloses = new AtomicInteger(0);
		fErrorState = null;
		fIsDone = false;
		fIsOpen = true;

		fDumpPinPoint = new Date();

		PrintStream			logFile = null;
		if ( context.getLogPath() != null )
		{
			logFile = new PrintStream(new FileOutputStream(context.getLogPath()));
		}
		fLogFile = logFile;

		GenericIOParameters 		parameters = new GenericIOParameters().port(context.getPort()).ssl(false);
		fServer = GenericIOFactory.makeServer(new InternalListener(false), parameters);

		GenericIOServer<ImpSCServerConnection> 		monitor = null;
		if ( context.getMonitorPort() != 0 )
		{
			GenericIOParameters 		contextParameters = new GenericIOParameters().port(context.getMonitorPort()).ssl(false);
			monitor = GenericIOFactory.makeServer(new InternalListener(true), contextParameters);
			System.out.println("Monitor active on port " + context.getMonitorPort());
			log("Monitor active on port " + context.getMonitorPort(), null, true);
		}
		fMonitor = monitor;

		fTransactionCount = new AtomicLong(0);

		fLastGetTimesIndex = new AtomicInteger(0);
		fLastGetTimes = new AtomicReferenceArray<String>(LAST_GET_TIMES_QTY);
		for ( int i = 0; i < LAST_GET_TIMES_QTY; ++i )
		{
			fLastGetTimes.set(i, "");
		}

		if ( fMonitor != null )
		{
			fMonitor.start();
		}
		fServer.start();

		System.out.println("Server active on port " + context.getPort());
		log("Server started and active on port " + context.getPort(), null, true);
	}

	@Override
	public SCNotifications getNotificationHandler()
	{
		return null;
	}

	@Override
	public void close()
	{
		shutdown();
	}

	@Override
	public long getTTL(String key) throws Exception
	{
		SCDataSpec 	entry = getEntry(key, true);
		return (entry != null) ? entry.ttl : 0;
	}

	@Override
	public String getErrorState()
	{
		return fErrorState;
	}

	@Override
	public void notifyException(Exception e)
	{
		ImpSCUtils.handleException(e, this);
	}

	@Override
	public void log(String s, Throwable e, boolean newline)
	{
		Date			now = new Date();
		if ( newline )
		{
			s = now.toString() + ": " + s;
		}

		System.out.print(s);
		if ( newline )
		{
			System.out.println();
		}
		if ( e != null )
		{
			e.printStackTrace();
		}

		if ( fLogFile != null )
		{
			fLogFile.print(s);
			if ( newline )
			{
				fLogFile.println();
			}
			if ( e != null )
			{
				e.printStackTrace(fLogFile);
			}
		}
	}

	@Override
	public void 		writeKeyData(String filePath) throws IOException
	{
		fDatabase.writeKeyData(new File(filePath));
	}

	@Override
	public List<String> stackTrace()
	{
		List<String>						tab = new ArrayList<String>();
		Map<Thread, StackTraceElement[]>	threads = Thread.getAllStackTraces();
		for ( Thread thread : threads.keySet() )
		{
			String 		thread_name = thread.getName();
			tab.add(thread_name);

			StackTraceElement[]		stack = threads.get(thread);
			for ( StackTraceElement element : stack )
			{
				tab.add("\t" + element.getClassName() + " - " + element.getMethodName() + "() Line " + element.getLineNumber());
			}
		}

		return tab;
	}

	@Override
	public List<String> getConnectionList()
	{
		List<List<String>>								displayGrid = new ArrayList<List<String>>();
		List<AtomicInteger>								maxColumnWidths = new ArrayList<AtomicInteger>();
		for ( int i = 0; i < ConnectionListColumn.values().length; ++i )
		{
			displayGrid.add(new ArrayList<String>());
			maxColumnWidths.add(new AtomicInteger(0));
		}

		List<GenericIOClient<ImpSCServerConnection>> 	clients = fServer.getClients();

		addToConnectionList(ConnectionListColumn.ADDRESS, "Address", displayGrid, maxColumnWidths);
		addToConnectionList(ConnectionListColumn.AGE, "Age", displayGrid, maxColumnWidths);
		addToConnectionList(ConnectionListColumn.STATUS, "Status", displayGrid, maxColumnWidths);
		addToConnectionList(ConnectionListColumn.LAST_COMMAND_TIME, "Last I/O", displayGrid, maxColumnWidths);
		addToConnectionList(ConnectionListColumn.ADDRESS, "=======", displayGrid, maxColumnWidths);
		addToConnectionList(ConnectionListColumn.AGE, "===", displayGrid, maxColumnWidths);
		addToConnectionList(ConnectionListColumn.STATUS, "======", displayGrid, maxColumnWidths);
		addToConnectionList(ConnectionListColumn.LAST_COMMAND_TIME, "=================", displayGrid, maxColumnWidths);

		int			index = 0;
		for ( GenericIOClient<ImpSCServerConnection> connection : clients )
		{
			String		address = connection.getUserValue().toString();
			long		age = (System.currentTimeMillis() - connection.getUserValue().getCreationTime()) / (1000 * 60);
			long		last_command_time = (System.currentTimeMillis() - connection.getUserValue().getLastCommandTime()) / (1000 * 60);
			String 		status = connection.getUserValue().getCurrentCommand();

			addToConnectionList(ConnectionListColumn.ADDRESS, ++index + ". " + address, displayGrid, maxColumnWidths);
			addToConnectionList(ConnectionListColumn.AGE, Long.toString(age) + " minute(s)", displayGrid, maxColumnWidths);
			addToConnectionList(ConnectionListColumn.STATUS, status, displayGrid, maxColumnWidths);
			addToConnectionList(ConnectionListColumn.LAST_COMMAND_TIME, Long.toString(last_command_time) + " minute(s) ago", displayGrid, maxColumnWidths);
		}

		List<String>				tab = new ArrayList<String>();
		tab.add(clients.size() + " client(s)");
		for ( int i = 0; i < (clients.size() + 2); ++i )	// +2 for the header lines
		{
			StringBuilder		thisLine = new StringBuilder();
			for ( ConnectionListColumn column : ConnectionListColumn.values() )
			{
				List<String> 	columnValues = displayGrid.get(column.ordinal());
				String			value = columnValues.get(i);
				int				maxWidth = maxColumnWidths.get(column.ordinal()).get();
				thisLine.append(value);
				for ( int j = value.length(); j <= maxWidth; ++j )
				{
					thisLine.append(" ");
				}
			}
			tab.add(thisLine.toString());
		}

		return tab;
	}

	/**
	 * Set the error state. IMPORTANT: Setting the error state will cause a shutdown of the non-monitor server
	 *
	 * @param errorState new error
	 */
	@Override
	public void setErrorState(String errorState)
	{
		assert errorState != null;
		fErrorState = errorState;
		shutdown();
	}

	@Override
	public synchronized void shutdown()
	{
		if ( !fIsDone )
		{
			fIsDone = true;
			fServer.close();
		}
		notifyAll();
	}

	@Override
	public synchronized void		join() throws InterruptedException
	{
		while ( !fIsDone && fIsOpen )
		{
			wait();
		}

		internalClose();
	}

	@Override
	public ChunkedByteArray get(String key, boolean ignoreTTL)
	{
		TrackerTimer		timer = new TrackerTimer(fGetTimerData);
		timer.start();

		SCDataSpec 	entry = getEntry(key, ignoreTTL);

		int 		getTime = timer.end("get()");
		String 		timingMessage;
		if ( (entry != null) && (entry.data != null) )
		{
			int 		bytesPerSecond = (entry.data.size() / Math.max(1, getTime));
			timingMessage = getTime + " ms " + entry.data.size() + " bytes " + bytesPerSecond + " bpms";
		}
		else
		{
			timingMessage = getTime + " ms <not found>";
		}

		int 		index = fLastGetTimesIndex.getAndIncrement();
		if ( index >= LAST_GET_TIMES_QTY )
		{
			fLastGetTimesIndex.compareAndSet(index + 1, 0);
			index = 0;
		}
		fLastGetTimes.set(index, timingMessage);

		return (entry != null) ? entry.data : null;
	}

	@Override
	public void putWithBackup(String key, SCDataSpec data, SCGroupSpec groups) throws Exception
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void put(String key, SCDataSpec data, SCGroupSpec groups)
	{
		TrackerTimer		timer = new TrackerTimer(fPutTimerData);
		timer.start();

		try
		{
			fDatabase.put(key, data, groups);
		}
		catch ( Throwable e )
		{
			ImpSCUtils.handleException(e, null);
		}

		timer.end("put()");
	}

	@Override
	public List<String> listGroup(SCGroup group)
	{
		try
		{
			return fDatabase.listGroup(group);
		}
		catch ( Throwable e )
		{
			ImpSCUtils.handleException(e, null);
		}

		return new ArrayList<String>();
	}

	@Override
	public List<String> removeGroup(SCGroup group)
	{
		try
		{
			return fDatabase.removeGroup(group);
		}
		catch ( Throwable e )
		{
			ImpSCUtils.handleException(e, null);
		}

		return new ArrayList<String>();
	}

    @Override
    public List<String> regExFindKeys(String expression) throws Exception
    {
		Set<String> 			keySet = null;
		try
		{
			keySet = fDatabase.regexFindKeys(expression);
		}
		catch ( Throwable e )
		{
			ImpSCUtils.handleException(e, null);
		}

		return new ArrayList<String>(keySet);
	}

    @Override
	public List<String> regExRemove(String expression)
	{
		Set<String> 			keySet = null;
		try
		{
			keySet = fDatabase.regexFindKeys(expression);
			for ( String thisKey : keySet )
			{
				fDatabase.remove(thisKey);
			}
		}
		catch ( Throwable e )
		{
			ImpSCUtils.handleException(e, null);
		}

		return new ArrayList<String>(keySet);
	}

	@Override
	public void remove(String key)
	{
		try
		{
			fDatabase.remove(key);
		}
		catch ( Throwable e )
		{
			ImpSCUtils.handleException(e, null);
		}
	}

	@Override
	public void			incrementTransactionCount()
	{
		fTransactionCount.incrementAndGet();
	}

	@Override
	public List<String> dumpStats(boolean verbose)
	{
		List<String>	tab = new ArrayList<String>();
		int				client_count = fServer.getClients().size();

		try
		{
			tab.add("Stats since: " + fDumpPinPoint);
			tab.add("Cache server build: " + CHECKIN_VERSION);
			tab.add("===========================");

			TrackerTimer.output(tab, fGetTimerData, verbose);
			TrackerTimer.output(tab, fPutTimerData, verbose);
			if ( verbose )
			{
				tab.add(" ");
			}
			tab.addAll(fDatabase.dumpStats(verbose));

			long 			minutesRunning = Math.max((System.currentTimeMillis() - fDumpPinPoint.getTime()) / (1000 * 60), 1);

			tab.add("Client Count:            " + client_count);
			tab.add("VM Free Memory:          " + Runtime.getRuntime().freeMemory());
			tab.add("VM Total Memory:         " + Runtime.getRuntime().totalMemory());
			tab.add("VM Max Memory:           " + Runtime.getRuntime().maxMemory());
			tab.add("VM Version:              " + System.getProperty("java.vm.version", "???"));
			tab.add("Thread Count:            " + Thread.activeCount());
			tab.add("Current Time:            " + (new Date()));
			tab.add("Transaction Qty:         " + fTransactionCount.get());
			tab.add("Transactions Per Minute: " + (fTransactionCount.get() / minutesRunning));
			tab.add("Abnormal Disconnects:    " + fAbnormalCloses.get());

			tab.add(" ");

			tab.add("Last " + LAST_GET_TIMES_QTY + " get times:");
			for ( int i = 0; i < LAST_GET_TIMES_QTY; ++i )
			{
				String s = fLastGetTimes.get(i);
				if ( s.length() > 0 )
				{
					tab.add(s);
				}
			}

			tab.add(" ");
		}
		catch ( IOException e )
		{
			ImpSCUtils.handleException(e, null);
		}

		return tab;
	}

	private enum ConnectionListColumn
	{
		ADDRESS,
		AGE,
		STATUS,
		LAST_COMMAND_TIME
	}

	private void		addToConnectionList(ConnectionListColumn column, String value, List<List<String>> displayGrid, List<AtomicInteger> maxColumnWidths)
	{
		List<String>		columnValues = displayGrid.get(column.ordinal());
		columnValues.add(value);

		AtomicInteger		maxWidth = maxColumnWidths.get(column.ordinal());
		int 				value_length = value.length() + 2;	// margin is 2
		if ( value_length > maxWidth.get() )
		{
			maxWidth.set(value_length);
		}
	}

	private SCDataSpec getEntry(String key, boolean ignoreTTL)
	{
		SCDataSpec entry = null;
		try
		{
			entry = fDatabase.get(key);

			if ( entry != null )
			{
				if ( !ignoreTTL )
				{
					long			now = System.currentTimeMillis();
					if ( now >= entry.ttl )
					{
						entry = null;
						fDatabase.remove(key);
					}
				}
			}
		}
		catch ( Throwable e )
		{
			ImpSCUtils.handleException(e, null);
		}
		return entry;
	}

	private synchronized void	internalClose()
	{
		if ( !fIsOpen )
		{
			return;
		}
		fIsOpen = false;

		log("Shutting down server.", null, true);

		if ( fLogFile != null )
		{
			fLogFile.flush();
		}

		// first thing - stop accepting clients
		noMoreConnections();

		try
		{
			fDatabase.close();
		}
		catch ( Exception e )
		{
			ImpSCUtils.handleException(e, null);
		}

		log("Server shutdown complete.", null, true);

		if ( fErrorState != null )
		{
			log("Going into zombie mode due to error state: " + fErrorState, null, true);
			try
			{
				Thread.sleep(Long.MAX_VALUE);
			}
			catch ( InterruptedException e )
			{
				// ignore
			}
		}
		else
		{
			if ( fLogFile != null )
			{
				fLogFile.close();
			}
		}
		notifyAll();
	}

	private synchronized void noMoreConnections()
	{
		fServer.close();
		closeMonitor();
	}

	private synchronized void closeMonitor()
	{
		if ( (fMonitor != null) && (fErrorState == null) )
		{
			fMonitor.close();
		}
	}

	private class InternalListener implements GenericIOServerListener<ImpSCServerConnection>, GenericIOClientPoolListener<ImpSCServerConnection>
	{
		public InternalListener(boolean isMonitor)
		{
			fIsMonitor = isMonitor;
		}

		@Override
		public boolean staleClientClosing(GenericIOClient<ImpSCServerConnection> client)
		{
			log("Stale client closing: " + client.toString(), null, true);
			return true;
		}

		@Override
		public void notifyServerClosing(GenericIOServer<ImpSCServerConnection> server) throws Exception
		{
		}

		@Override
		public void notifyException(GenericIOServer<ImpSCServerConnection> server, Exception e)
		{
			if ( !ImpSCUtils.handleException(e, ImpSCServer.this) )
			{
				fAbnormalCloses.incrementAndGet();
			}
		}

		@Override
		public void notifyClientAccepted(GenericIOServer<ImpSCServerConnection> server, GenericIOClient<ImpSCServerConnection> client) throws Exception
		{
			ImpSCServerConnection connection = new ImpSCServerConnection(ImpSCServer.this, client, fIsMonitor);
			client.setUserValue(connection);

			GenericIOLineProcessor<ImpSCServerConnection> processor = new GenericIOLineProcessor<ImpSCServerConnection>(client, connection);
			processor.execute();
		}

		private final boolean fIsMonitor;
	}

	private static final TrackerTimer.data 		fGetTimerData = new TrackerTimer.data("Gets");
	private static final TrackerTimer.data 		fPutTimerData = new TrackerTimer.data("Puts");

	private static final int				LAST_GET_TIMES_QTY = 50;

	private static final String 			CHECKIN_VERSION = "1.5";

	private final SCStorage 											fDatabase;
	private final AtomicInteger 										fAbnormalCloses;
	private final GenericIOServer<ImpSCServerConnection> 				fServer;
	private final GenericIOServer<ImpSCServerConnection>				fMonitor;
	private final AtomicReferenceArray<String>							fLastGetTimes;
	private	final AtomicInteger											fLastGetTimesIndex;
	private	final Date 													fDumpPinPoint;
	private final AtomicLong 											fTransactionCount;
	private final PrintStream											fLogFile;
	private boolean 													fIsDone;
	private boolean 													fIsOpen;
	private volatile String												fErrorState;
}