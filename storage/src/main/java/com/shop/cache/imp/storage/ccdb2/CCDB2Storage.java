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
package com.shop.cache.imp.storage.ccdb2;

import com.shop.cache.api.storage.SCStorage;
import com.shop.cache.api.storage.SCStorageServerDriver;
import com.shop.cache.api.common.SCDataSpec;
import com.shop.cache.api.common.SCGroup;
import com.shop.cache.api.common.SCGroupSpec;
import com.shop.util.ccdb2.CCDB2DataSpec;
import com.shop.util.ccdb2.CCDB2Driver;
import com.shop.util.ccdb2.CCDB2Instance;
import com.shop.util.ccdb2.CCDB2SetFileLengthException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * SHOP.COM's Storage implementation
 *
 * @author Jordan Zimmerman
 */
class CCDB2Storage implements SCStorage, CCDB2Driver
{
	CCDB2Storage(CCDB2Parameters parameters)
	{
		fParameters = parameters.clone();
		fGetQty = new AtomicLong(0);
		fSuccessfulGetQty = new AtomicLong(0);
		fPutQty = new AtomicLong(0);
		fLock = new ReentrantReadWriteLock();
		fInstances = new ArrayList<CCDB2Instance>();

		fDriver = new SCStorageServerDriver()
		{
			@Override
			public void log(String s, Throwable e, boolean addNewline)
			{
				// do nothing
			}

			@Override
			public void notifyException(Exception e)
			{
				// do nothing
			}

			@Override
			public void setErrorState(String errorState)
			{
				// do nothing
			}

			@Override
			public void remove(String key)
			{
				// do nothing
			}
		};

		fCleanupThread = new Thread
		(
			new Runnable()
			{
				@Override
				public void run()
				{
					boolean 		firstTime = true;
					while ( !Thread.currentThread().isInterrupted() )
					{
						try
						{
							Thread.sleep(firstTime ? FIRST_TIME_SLEEP_TICKS : SLEEP_TICKS);
							firstTime = false;
						}
						catch ( InterruptedException dummy )
						{
							break;
						}

						doCleanupTasks();
					}
				}
			}
		);
		fCleanupThread.setDaemon(true);
	}

	@Override
	public void setStorageServerDriver(SCStorageServerDriver driver)
	{
		fDriver = driver;
	}

	@Override
	public void handleException(Exception e)
	{
		fDriver.notifyException(e);
	}

	@Override
	public void log(String s, Throwable e, boolean newline)
	{
		fDriver.log(s, e, newline);
	}

	@Override
	public String getDBExtension()
	{
		return fParameters.getDBFileExtension();
	}

	@Override
	public String getIndexExtension()
	{
		return fParameters.getIndexFileExtension();
	}

	@Override
	public boolean doChunking()
	{
		return true;
	}

	@Override
	public void callRemoveObject(String s)
	{
		fDriver.remove(s);
	}

	@Override
	public long getAllocationChunkSize()
	{
		return 0;
	}

	@Override
	public void open(File path) throws IOException
	{
		fDirectoryPath = path;
		fDirectoryPath.mkdirs();

		List<CCDB2Instance> 	localList = new ArrayList<CCDB2Instance>();
		File[]					files = fDirectoryPath.listFiles
		(
			new FilenameFilter()
			{
				@Override
				public boolean accept(File dir, String name)
				{
					return name.toLowerCase().endsWith(fParameters.getDBFileExtension());
				}
			}
		);

		for ( File f : files )
		{
			try
			{
				int				dotIndex = f.getName().indexOf('.');
				String 			unextendedName = (dotIndex > 0) ? f.getName().substring(0, dotIndex) : f.getName();
				CCDB2Instance 	instance = new CCDB2Instance(this, f.getParentFile(), unextendedName, fParameters.getBackgroundPutLength());
				if ( (System.currentTimeMillis() - instance.getCreationTime()) < (fParameters.getMaxAgeMilliseconds() * 2) )
				{
					localList.add(instance);
				}
				else
				{
					instance.delete();
				}
			}
			catch ( CCDB2Instance.OldFileException e )
			{
				System.out.println(e.getMessage());
			}
			catch ( IOException e )
			{
				log("", e, true);
			}
		}

		Collections.sort
		(
			localList,
			new Comparator<CCDB2Instance>()
			{
				@Override
				public int compare(CCDB2Instance o1, CCDB2Instance o2)
				{
					long diff = o1.getCreationTime() - o2.getCreationTime();
					return (diff < 0) ? -1 : ((diff > 0) ? 1 : 0);
				}
			}
		);

		if ( localList.size() == 0 )
		{
			CCDB2Instance 		newInstance = makeNewInstance();
			localList.add(newInstance);
		}

		while ( localList.size() > fParameters.getMaxInstances() )
		{
			CCDB2Instance		instance = localList.remove(0);
			instance.delete();
		}

		CCDB2InstanceLoader loader = new CCDB2InstanceLoader
		(
			this, 
			localList,
			new CCDB2InstanceLoader.ProcessDriver()
			{
				@Override
				public void process(CCDB2Instance instance, AtomicInteger percentDone) throws IOException
				{
					instance.loadFile(percentDone);
				}
			}
		);
		log("Loading...", null, true);
		loader.load();

		fInstances.clear();
		fInstances.addAll(localList);

		fCleanupThread.start();
	}

	@Override
	public SCDataSpec get(String key) throws IOException
	{
		fGetQty.incrementAndGet();

		CCDB2DataSpec 	ccdb2Spec;

		fLock.readLock().lock();
		try
		{
			AtomicReference<Boolean> 		wasDeleted = new AtomicReference<Boolean>(false);
			ListIterator<CCDB2Instance> iterator = reverseIterator();
			ccdb2Spec = null;
			while ( (ccdb2Spec == null) && iterator.hasPrevious() )
			{
				CCDB2Instance instance = iterator.previous();
				if ( wasDeleted.get() )
				{
					instance.removeFromIndex(key);
				}
				else
				{
					ccdb2Spec = instance.get(key, wasDeleted);
				}
			}
		}
		finally
		{
			fLock.readLock().unlock();
		}

		if ( ccdb2Spec != null )
		{
			fSuccessfulGetQty.incrementAndGet();
			return new SCDataSpec(ccdb2Spec.data, ccdb2Spec.ttl);
		}

		return null;
	}

	@Override
	public void put(String key, SCDataSpec spec, SCGroupSpec groups)
	{
		if ( (key.length() == 0) || (spec.data.size() == 0) )
		{
			return;
		}

		CCDB2DataSpec 		ccdb2Spec = new CCDB2DataSpec(spec.data, spec.ttl);
		long[]				ccdb2Groups = (groups != null) ? new long[groups.size()] : null;
		if ( groups != null )
		{
			for ( int i = 0; i < groups.size(); ++i )
			{
				ccdb2Groups[i] = groups.get(i).value;
			}
		}

		fPutQty.incrementAndGet();

		fLock.readLock().lock();
		try
		{
			boolean 						isFirst = true;
			ListIterator<CCDB2Instance> 	iterator = reverseIterator();
			while ( iterator.hasPrevious() )
			{
				CCDB2Instance		instance = iterator.previous();
				try
				{
					if ( isFirst )
					{
						isFirst = false;
						instance.put(key, ccdb2Spec, ccdb2Groups);
					}
					else
					{
						instance.removeFromIndex(key);
					}
				}
				catch ( CCDB2SetFileLengthException e )
				{
					fDriver.setErrorState("Out of Disk Space");
				}
				catch ( IOException e )
				{
					handleException(e);
				}
			}
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public void close() throws IOException
	{
		log("DM closing", null, true);

		fCleanupThread.interrupt();
		try
		{
			fCleanupThread.join();
		}
		catch ( InterruptedException ignore )
		{
		}

		fLock.writeLock().lock();
		try
		{
			CCDB2InstanceLoader		loader = new CCDB2InstanceLoader
			(
				this,
				fInstances,
				new CCDB2InstanceLoader.ProcessDriver()
				{
					@Override
					public void process(CCDB2Instance instance, AtomicInteger percentDone) throws IOException
					{
						instance.close(percentDone);
					}
				}
			);
			loader.load();

			fInstances.clear();
		}
		finally
		{
			fLock.writeLock().unlock();
		}
	}

	@Override
	public Set<String> regexFindKeys(String regex)
	{
		Set<String>						keys = new HashSet<String>();
		try
		{
			Pattern p = Pattern.compile(regex);

			fLock.readLock().lock();
			try
			{
				for ( CCDB2Instance instance : fInstances )
				{
					instance.regexFindKeys(p, keys);
				}
			}
			finally
			{
				fLock.readLock().unlock();
			}
		}
		catch ( PatternSyntaxException e )
		{
			log("Bad Regular Expression", e, true);	// safe to ignore. Treat as a zero result match
		}
		return keys;
	}

	@Override
	public void remove(String key) throws IOException
	{
		fLock.readLock().lock();
		try
		{
			boolean 						isFirst = true;
			ListIterator<CCDB2Instance> 	iterator = reverseIterator();
			while ( iterator.hasPrevious() )
			{
				CCDB2Instance 	instance = iterator.previous();
				if ( isFirst )
				{
					isFirst = false;
					instance.remove(key);
				}
				else
				{
					instance.removeFromIndex(key);
				}
			}
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public List<String> listGroup(SCGroup group) throws IOException
	{
		List<String>		keys = new ArrayList<String>();
		fLock.readLock().lock();
		try
		{
			for ( CCDB2Instance instance : fInstances )
			{
				List<String> 		instanceKeys =	instance.listGroup(group.value);
				keys.addAll(instanceKeys);
			}
		}
		finally
		{
			fLock.readLock().unlock();
		}

		return keys;
	}

	@Override
	public List<String> removeGroup(SCGroup group) throws IOException
	{
		List<String>		keys = new ArrayList<String>();
		fLock.readLock().lock();
		try
		{
			for ( CCDB2Instance instance : fInstances )
			{
				List<String> 		instanceKeys =	instance.removeGroup(group.value);
				keys.addAll(instanceKeys);
			}
		}
		finally
		{
			fLock.readLock().unlock();
		}

		return keys;
	}

	@Override
	public List<String> dumpStats(boolean verbose) throws IOException
	{
		List<String>	tab = new ArrayList<String>();
		long			successful_get_qty = fSuccessfulGetQty.get();
		long			put_qty = fPutQty.get();
		long			total_qty = successful_get_qty + put_qty;

		ByteArrayOutputStream	work = new ByteArrayOutputStream();
		PrintStream				out = new PrintStream(work);

		fLock.readLock().lock();
		try
		{
			for ( CCDB2Instance instance : fInstances )
			{
				instance.dumpStats(out);
			}
		}
		finally
		{
			fLock.readLock().unlock();
		}

		out.close();
		BufferedReader		in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(work.toByteArray())));
		for(;;)
		{
			String		line = in.readLine();
			if ( line == null )
			{
				break;
			}
			tab.add(line);
		}

		tab.add("");

		tab.add("Total Gets:  " + fGetQty.get());
		tab.add("Gets:        " + successful_get_qty);
		tab.add("Puts:        " + put_qty);
		if ( total_qty > 0 )
		{
			tab.add("Gets v Puts: " + ((successful_get_qty * 100) / total_qty) + "%");
		}

		tab.add("");

		return tab;
	}

	@Override
	public void writeKeyData(File f) throws IOException
	{
		PrintStream			out = new PrintStream(new BufferedOutputStream(new FileOutputStream(f), 0x10000));
		out.println("File\tCacheKey\tAddress\tTTL");

		try
		{
			fLock.readLock().lock();
			try
			{
				for ( CCDB2Instance instance : fInstances )
				{
					instance.writeKeyData(out);
				}
			}
			finally
			{
				fLock.readLock().unlock();
			}
		}
		finally
		{
			out.close();
		}
	}
	
	private void doCleanupTasks()
	{
		cleanupInstances();
		cleanupOldObjects();
	}

	private void cleanupOldObjects()
	{
		fLock.readLock().lock();
		try
		{
			// important - do this from oldest to newest so that old objects don't suddenly become active
			for ( CCDB2Instance instance : fInstances )
			{
				instance.removeOldKeysFromIndex();
			}
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	private void cleanupInstances()
	{
		/**
		 * allocate a new instance just in case it's needed - that way it
		 * won't be done while synchronized
		 */
		CCDB2Instance 	newInstance;
		try
		{
			newInstance = makeNewInstance();
		}
		catch ( IOException e )
		{
			handleException(e);
			return;
		}

		CCDB2Instance 		deleteInstance = null;

		fLock.writeLock().lock();
		try
		{
			if ( fInstances.size() > 0 )
			{
				CCDB2Instance 		mainInstance = fInstances.get(fInstances.size() - 1);
				if ( (System.currentTimeMillis() - mainInstance.getCreationTime()) >= fParameters.getShuffleMilliseconds() )
				{
					if ( fInstances.size() >= fParameters.getMaxInstances() )
					{
						deleteInstance = fInstances.remove(0);
					}

					fInstances.add(newInstance);
					newInstance = null;	// so it won't be deleted
				}
			}
		}
		finally
		{
			fLock.writeLock().unlock();
		}

		if ( newInstance != null )
		{
			try
			{
				newInstance.delete();
			}
			catch ( IOException e )
			{
				handleException(e);
			}
		}

		if ( deleteInstance != null )
		{
			try
			{
				deleteInstance.delete();
			}
			catch ( IOException e )
			{
				handleException(e);
			}
		}
	}

	private ListIterator<CCDB2Instance> reverseIterator()
	{
		return fInstances.listIterator(fInstances.size());
	}

	private CCDB2Instance makeNewInstance() throws IOException
	{
		String			filename = fParameters.getFilePrefix() + System.nanoTime();
		return new CCDB2Instance(this, fDirectoryPath, filename, fParameters.getBackgroundPutLength());
	}

	private static final int			SLEEP_TICKS = 15 * 60 * 1000;	// 15 minutes
	private static final int			FIRST_TIME_SLEEP_TICKS = 2 * 60 * 1000;	// 2 minutes

	private	File 								fDirectoryPath;
	private final AtomicLong 					fGetQty;
	private final AtomicLong 					fSuccessfulGetQty;
	private final AtomicLong 					fPutQty;
	private final List<CCDB2Instance> 			fInstances;
	private final ReentrantReadWriteLock 		fLock;
	private final Thread 						fCleanupThread;
	private final CCDB2Parameters 				fParameters;
	private SCStorageServerDriver fDriver;
}
