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
package com.shop.util.ccdb2;

import com.shop.util.chunked.ChunkedByteArray;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Main API for CCDB2<br>
 *
 * @author Jordan Zimmerman
 * @since 1.1 JLZ 12/30/2008 - delete() wasn't closing the instance. This caused a resource leak (the background put thread).
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "SynchronizationOnLocalVariableOrMethodParameter"})
public class CCDB2Instance implements CCDB2UpdateIndexInterface
{
	/**
	 * Thrown when an incompatible file is encountered
	 */
	public class OldFileException extends IOException
	{
		public OldFileException()
		{
		}

		public OldFileException(String s)
		{
			super(s);
		}
	}

	/**
	 * Opens/Creates a CCDB2 instance. If the file already exists it's opened. If it doesn't exist it's created.
	 *
	 * @param driver driver instance
	 * @param directory Directory to create/read files
	 * @param baseFilename base file name for the files
	 * @throws IOException errors
	 */
	public CCDB2Instance(CCDB2Driver driver, File directory, String baseFilename) throws IOException
	{
		this(driver, directory, baseFilename, 0);
	}

	/**
	 * Opens/Creates a CCDB2 instance. If the file already exists it's opened. If it doesn't exist it's created.
	 *
	 * @param driver driver instance
	 * @param directory Directory to create/read files
	 * @param baseFilename base file name for the files
	 * @param pendingPutQueueLength length for the background put queue. If greater than 0, put writes will occur in a background
	 * thread but the queue will block if it gets longer than the specified length
	 * @throws IOException errors
	 */
	public CCDB2Instance(CCDB2Driver driver, File directory, String baseFilename, int pendingPutQueueLength) throws IOException
	{
		directory.mkdirs();

		fDriver = driver;
		fFilePath = new File(directory, baseFilename + fDriver.getDBExtension()).getPath();
		fIsOpen = new AtomicBoolean(true);
		fUseCount = 0;

		fFile = new CCDB2File(driver, fFilePath, (byte)DEAD_BYTE);
		readHeader();

		fIndex = new ConcurrentHashMap<String, CCDB2IndexEntry>();
		fGroupsIndex = new ConcurrentHashMap<Long, HashSet<String>>();
		fIndexSize = new AtomicLong(0);
		fGroupsIndexSize = new AtomicLong(0);

		fInMemoryGetQty = new AtomicLong(0);
		fFromDiskGetQty = new AtomicLong(0);
		fPendingPutQueueOverflowQty = new AtomicLong(0);

		fIndexFile = new CCDB2IndexFile(fDriver, new File(directory, baseFilename + fDriver.getIndexExtension()));

		fActivePendingPut = new AtomicReference<ActivePendingPut>(null);
		fPendingPutException = new AtomicReference<IOException>(null);
		fPendingPutQueue = (pendingPutQueueLength > 0) ? new LinkedBlockingQueue<PendingPutRecord>(pendingPutQueueLength) : null;
		fPendingPutQueueThread = (pendingPutQueueLength > 0) ? new Thread(new PendingPutThread()) : null;
		if ( fPendingPutQueueThread != null )
		{
			fPendingPutQueueThread.start();
		}
	}

	/**
	 * Load the file. This should always be called - i.e. even if it's a new file.
	 *
	 * @param percentDone reporting mechanism. Will get updated with the percentage of the file that's loaded.
	 * @throws IOException errors
	 */
	public void loadFile(AtomicInteger percentDone) throws IOException
	{
		fIndexFile.load(fIndex, fGroupsIndex, percentDone, this);
	}

	/**
	 * Returns the name of the main DB file (not the index)
	 *
	 * @return name
	 */
	public String getDatabaseName()
	{
		return fFilePath;
	}

	/**
	 * Return the ticks when the file was created
	 *
	 * @return creation ticks
	 */
	public long getCreationTime()
	{
		return fCreationDate;
	}

	/**
	 * list the objects associated with the given group
	 *
	 * @param groupSpec ID of the group
	 * @return list of keys
	 * @throws IOException errors
	 */
	public List<String> listGroup(long groupSpec) throws IOException
	{
		List<String> 	keysList = new ArrayList<String>();

		updateUseCount(true);
		try
		{
			if ( fIsOpen.get() )
			{
				Set<String> 	list = fGroupsIndex.get(groupSpec);
				if ( list != null )
				{
					synchronized(list)
					{
						keysList.addAll(list);
					}
				}
			}
		}
		finally
		{
			updateUseCount(false);
		}

		return keysList;
	}

	/**
	 * Remove the objects associated with the given group
	 *
	 * @param groupSpec ID of the group
	 * @return list of keys removed
	 * @throws IOException errors
	 */
	public List<String> removeGroup(long groupSpec) throws IOException
	{
		List<String> 	keysList = new ArrayList<String>();

		updateUseCount(true);
		try
		{
			if ( fIsOpen.get() )
			{
				Set<String> 	list = fGroupsIndex.get(groupSpec);
				if ( list != null )
				{
					Set<String> 	copyList;
					synchronized(list)
					{
						copyList = new HashSet<String>(list);
					}

					for ( String key : copyList )
					{
						fDriver.callRemoveObject(key);
						keysList.add(key);
					}
				}
			}
		}
		finally
		{
			updateUseCount(false);
		}

		return keysList;
	}

	/**
	 * Writes index information to the given stream. The format is:
	 * [DB Path] -tab- [Key] -tab- [Address] -tab- [Object TTL] -newline-
	 *
	 * @param out stream to write to
	 */
	public void writeKeyData(PrintStream out)
	{
		updateUseCount(true);
		try
		{
			if ( !fIsOpen.get() )
			{
				return;
			}

			for ( String key : fIndex.keySet() )
			{
				CCDB2IndexEntry 		entry = fIndex.get(key);
				if ( entry != null )
				{
					String 	fixedKey = key.replace("\t", " ");
					out.println(fFilePath + "\t" + fixedKey + "\t" + entry.address + "\t" + (fCreationDate + entry.TTLDelta));
				}
			}
		}
		finally
		{
			updateUseCount(false);
		}
	}

	/**
	 * Utility - purges the in memory index of stale keys
	 *
	 * @return qty purged
	 */
	public int 	removeOldKeysFromIndex()
	{
		int		qty = 0;
		long	now = System.currentTimeMillis();

		updateUseCount(true);
		try
		{
			if ( fIsOpen.get() )
			{
				Iterator<String> 	iterator = fIndex.keySet().iterator();
				while ( iterator.hasNext() )
				{
					String					key = iterator.next();
					CCDB2IndexEntry 		entry = fIndex.get(key);
					if ( (entry != null) && (entry.TTLDelta > 0) )
					{
						long 		actualTTL = fCreationDate + entry.TTLDelta;
						if ( now >= actualTTL )
						{
							++qty;
							iterator.remove();
							updateIndexSize(key, false);
						}
					}
				}
			}
		}
		finally
		{
			updateUseCount(false);
		}

		return qty;
	}

	/**
	 * Utility - removes the given key from the index only - the DB file isn't changed
	 *
	 * @param key key to remove
	 */
	public void removeFromIndex(String key)
	{
		updateUseCount(true);
		try
		{
			if ( !fIsOpen.get() )
			{
				return;
			}

			if ( fIndex.remove(key) != null )
			{
				updateIndexSize(key, false);
			}
		}
		finally
		{
			updateUseCount(false);
		}
	}

	/**
	 * Removes the object associated with the given key (if found)
	 *
	 * @param key key to remove
	 * @throws IOException errors
	 */
	public void	remove(String key) throws IOException
	{
		updateUseCount(true);
		try
		{
			if ( !fIsOpen.get() )
			{
				return;
			}

			CCDB2IndexEntry entry = fIndex.get(key);
			if ( entry != null )
			{
				synchronized(entry)
				{
					if ( entry.address >= CCDB2IndexEntry.MINIMUM_ACTIVE_ADDRESS )
					{
						deleteObject(entry.address);
					}
					entry.address = CCDB2IndexEntry.NOT_EXISTS_ADDRESS;
					entry.bytesRef = null;
					entry.TTLDelta = 0;
				}
			}
		}
		finally
		{
			updateUseCount(false);
		}
	}

	/**
	 * Add the given object to the DB
	 *
	 * @param key key
	 * @param spec object/TTL
	 * @param groupSpecs optional groups - can be null
	 * @throws IOException errors
	 */
	public void put(String key, CCDB2DataSpec spec, long[] groupSpecs) throws IOException
	{
		if ( spec.data.size() == 0 )
		{
			throw new IOException("Zero-sized objects are not supported");
		}

		if ( groupSpecs == null )
		{
			groupSpecs = NULL_GROUP_SPECS;
		}

		spec.data.lock();

		updateUseCount(true);
		try
		{
			if ( !fIsOpen.get() )
			{
				return;
			}

			boolean 			addToIndexFile = false;
			CCDB2IndexEntry 	newEntry = new CCDB2IndexEntry();
			newEntry.address = CCDB2IndexEntry.NOT_EXISTS_ADDRESS;
			newEntry.bytesRef = null;
			newEntry.TTLDelta = (int)(spec.ttl - fCreationDate);
			CCDB2IndexEntry 	entry = fIndex.putIfAbsent(key, newEntry);
			if ( entry == null )
			{
				updateIndexSize(key, true);
				entry = newEntry;
				addToIndexFile = true;
			}
			synchronized(entry)
			{
				if ( entry.TTLDelta != newEntry.TTLDelta )
				{
					entry.TTLDelta = newEntry.TTLDelta;
					addToIndexFile = true;
				}
				processPut(entry, key, spec, groupSpecs, addToIndexFile);
			}
		}
		finally
		{
			updateUseCount(false);
		}

		updateGroupIndex(key, groupSpecs);

		IOException 	exception = fPendingPutException.getAndSet(null);
		if ( exception != null )
		{
			throw exception;
		}
	}

	/**
	 * Returns the approximate size in bytes of the in-memory index
	 *
	 * @return size
	 */
	public long getIndexSize()
	{
		return fIndexSize.get();
	}

	/**
	 * Returns the approximate size in bytes of the in-memory groups index
	 *
	 * @return size
	 */
	public long getGroupsIndexSize()
	{
		return fGroupsIndexSize.get();
	}

	/**
	 * Returns the number of objects in the DB
	 *
	 * @return qty
	 */
	public int	getObjectQty()
	{
		return fIndex.size();
	}

	/**
	 * Returns true if there is an entry for the given key
	 *
	 * @param key key to check
	 * @return true/false
	 */
	public boolean	hasKey(String key)
	{
		updateUseCount(true);
		try
		{
			if ( fIsOpen.get() )
			{
				CCDB2IndexEntry 		entry = fIndex.get(key);
				if ( entry != null )
				{
					synchronized(entry)
					{
						return entry.address != CCDB2IndexEntry.NOT_EXISTS_ADDRESS;
					}
				}
			}
		}
		finally
		{
			updateUseCount(false);
		}

		return false;
	}

	/**
	 * Returns the object associated with the given key.
	 *
	 * @param key key for the object
	 * @param wasDeleted if the object was deleted, this is set to true (can be null)
	 * @return the object/ttl or null
	 * @throws IOException errors
	 */
	public CCDB2DataSpec get(String key, AtomicReference<Boolean> wasDeleted) throws IOException
	{
		CCDB2DataSpec		spec = null;

		updateUseCount(true);
		try
		{
			if ( !fIsOpen.get() )
			{
				return null;
			}

			ActivePendingPut 		activePendingPut = fActivePendingPut.get();
			if ( (activePendingPut != null) && activePendingPut.key.equals(key) )
			{
				return activePendingPut.spec;
			}

			ChunkedByteArray		data = null;

			CCDB2IndexEntry 		entry = fIndex.get(key);
			if ( entry != null )
			{
				synchronized(entry)
				{
					if ( entry.address == CCDB2IndexEntry.NOT_EXISTS_ADDRESS )
					{
						if ( wasDeleted != null )
						{
							wasDeleted.set(true);
						}
					}
					else
					{
						data = (entry.bytesRef != null) ? entry.bytesRef.get() : null;
						if ( data == null )
						{
							if ( entry.address >= CCDB2IndexEntry.MINIMUM_ACTIVE_ADDRESS )
							{
								data = readObject(entry.address);
								if ( data == null )
								{
									if ( wasDeleted != null )
									{
										wasDeleted.set(true);
									}
									entry.address = CCDB2IndexEntry.NOT_EXISTS_ADDRESS;
								}
								else
								{
									data.lock();
									entry.bytesRef = new SoftReference<ChunkedByteArray>(data);

									fFromDiskGetQty.incrementAndGet();
								}
							}
						}
						else
						{
							fInMemoryGetQty.incrementAndGet();
						}
					}
				}

				if ( data != null )
				{
					spec = new CCDB2DataSpec(data, entry.TTLDelta + fCreationDate);
				}
			}
		}
		finally
		{
			updateUseCount(false);
		}

		return spec;
	}

	/**
	 * Close and delete the instance
	 *
	 * @throws IOException errors
	 */
	public void		delete() throws IOException
	{
		close(new AtomicInteger());

		if ( fFilePath != null )
		{
			if ( fIndexFile != null )
			{
				fIndexFile.close();
				if ( fIndexFile.getFilePath().exists() && !fIndexFile.getFilePath().delete() )
				{
					throw new IOException("Could not delete: " + fIndexFile.getFilePath().getPath());
				}
				fIndexFile = null;
			}

			if ( !(new File(fFilePath)).delete() )
			{
				throw new IOException("Could not delete: " + fFilePath);
			}
			fFilePath = null;
		}
	}

	/**
	 * Close the instance
	 *
	 * @param percentDone reporting mechanism. Will get updated with the percentage of the file that's loaded.
	 * @throws IOException errors
	 */
	public void 		close(AtomicInteger percentDone) throws IOException
	{
		if ( fIsOpen.getAndSet(false) && (fFile != null) )
		{
			waitForNoUsers();

			if ( fPendingPutQueueThread != null )
			{
				fPendingPutQueueThread.interrupt();
				try
				{
					fPendingPutQueueThread.join();
				}
				catch ( InterruptedException e )
				{
					Thread.currentThread().interrupt();	// restore the interrupted bit
					throw new IOException(e);
				}
			}

			fFile.close();
			fIndexFile.close();
		}
		percentDone.set(100);
	}

	/**
	 * Dump diagnostics to the given stream
	 *
	 * @param out the stream
	 */
	public void dumpStats(PrintStream out)
	{
		updateUseCount(true);
		try
		{
			if ( !fIsOpen.get() )
			{
				return;
			}

			long 	inMemoryCount = fInMemoryGetQty.get();
			long 	fromDiskCount = fFromDiskGetQty.get();
			long	pendingPutQueueOverflowQty = fPendingPutQueueOverflowQty.get();
			long 	totalAccessCount = Math.max(inMemoryCount + fromDiskCount, 1);

			out.println(getDatabaseName());
			out.println("\tCreated:        " + new Date(fCreationDate));
			out.println("\tCRCs:           " + (fUseCRCs ? "on" : "off"));
			out.println("\tLogical Size:   " + ((fFile != null) ? fFile.getLogicalSize() : -1));
			out.println("\tActual Size:    " + ((fFile != null) ? fFile.getActualSize() : -1));
			out.println("\tFile Pool Size: " + ((fFile != null) ? fFile.getFilePoolSize() : -1));
			out.println("\tObject Qty:     " + fIndex.size());
			out.println("\tIndex Size:     " + fIndexSize.get() + " bytes (approx)");
			out.println("\tGroups Size:    " + fGroupsIndexSize.get() + " bytes (approx)");
			out.println("\tMemory Gets:    " + inMemoryCount);
			out.println("\tDisk Gets:      " + fromDiskCount);
			out.println("\tMem v Disk:     " + ((inMemoryCount * 100) / totalAccessCount) + "%");
			out.println("\tPut Overflows:  " + pendingPutQueueOverflowQty);
		}
		finally
		{
			updateUseCount(false);
		}
	}

	/**
	 * Find keys that match the given regular expression
	 *
	 * @param p regular expression
	 * @param keys set to update with found keys
	 */
	public void regexFindKeys(Pattern p, Set<String> keys)
	{
		updateUseCount(true);
		try
		{
			if ( !fIsOpen.get() )
			{
				return;
			}

			for ( String thisKey : fIndex.keySet() )
			{
				if ( p.matcher(thisKey).matches() )
				{
					keys.add(thisKey);
				}
			}
		}
		finally
		{
			updateUseCount(false);
		}
	}

	@Override
	public void 	updateIndexSize(String key, boolean add)
	{
		int 		size = key.length() + CCDB2IndexEntry.INDEX_BASE_SIZE;
		if ( !add )
		{
			size *= -1;
		}
		fIndexSize.addAndGet(size);
	}

	private void processPut(CCDB2IndexEntry entry, String key, CCDB2DataSpec spec, long[] groupSpecs, boolean addToIndexFile) throws IOException
	{
		ChunkedByteArray 		previous = (entry.bytesRef != null) ? entry.bytesRef.get() : null;
		PendingPutRecord		pendingPut = new PendingPutRecord(key, previous, entry, spec, groupSpecs, addToIndexFile);

		// though this is a SoftReference, a hard reference is held by spec.data in the pending record until it's actually written
		entry.bytesRef = new SoftReference<ChunkedByteArray>(spec.data);
		if ( fPendingPutQueue != null )
		{
			fPendingPutQueue.remove(pendingPut);
			if ( fPendingPutQueue.offer(pendingPut) )
			{
				pendingPut = null;
			}
		}

		if ( pendingPut != null )
		{
			processPendingPut(pendingPut);
		}
	}

	private void processPendingPut(PendingPutRecord put) throws IOException
	{
		boolean 		needsUpdating = true;
		boolean			localAddToIndexFile = put.addToIndexFile;

		fActivePendingPut.set(new ActivePendingPut(put.key, put.spec));
		try
		{
			if ( (put.entry.address >= CCDB2IndexEntry.MINIMUM_ACTIVE_ADDRESS) && (put.previousBytesRef != null) )
			{
				// as an object becomes stale, multiple app servers are likely to write the same object
				// at the same time. Ignore duplicates.
				if ( put.previousBytesRef.equals(put.spec.data) )
				{
					needsUpdating = false;
				}
			}

			if ( needsUpdating )
			{
				if ( put.entry.address >= CCDB2IndexEntry.MINIMUM_ACTIVE_ADDRESS )
				{
					CCDB2Record 	record = CCDB2Record.existingRecord(fFile, fUseCRCs, put.entry.address);
					record.load(fDriver, CCDB2Record.LoadMode.SIZES_ONLY);
					if ( (record.getObjectSize() >= put.spec.data.size()) && (record.getGroupSpecQty() >= put.groupSpecs.length) )
					{
						record.writeRecord(put.key, put.spec.data, put.groupSpecs);
						if ( record.getGroupSpecQty() > 0 )
						{
							localAddToIndexFile = true;	// can't take chance that the group specs haven't changed
						}
					}
					else
					{
						put.entry.address = CCDB2IndexEntry.NOT_EXISTS_ADDRESS;		// can't be overwritten
					}
				}

				if ( put.entry.address < CCDB2IndexEntry.MINIMUM_ACTIVE_ADDRESS )
				{
					localAddToIndexFile = true;
					put.entry.address = writeObject(put.key, put.spec.data, put.groupSpecs);
				}

				if ( localAddToIndexFile )
				{
					fIndexFile.addNewEntry(put.key, put.entry.address, put.entry.TTLDelta, put.groupSpecs);
				}
			}
		}
		finally
		{
			fActivePendingPut.set(null);
		}
	}

	private synchronized void waitForNoUsers()
	{
		while ( fUseCount > 0 )
		{
			fDriver.log("Waiting on " + fUseCount + " threads...", null, true);
			try
			{
				wait();
			}
			catch ( InterruptedException dummy )
			{
				break;
			}
		}
	}

	private void deleteObject(long address) throws IOException
	{
		CCDB2Record		record = CCDB2Record.existingRecord(fFile, fUseCRCs, address);
		record.markDeleted();
	}

	private void readHeader() throws IOException
	{
		CCDB2io		io = null;
		try
		{
			if ( fFile.getActualSize() == 0 )
			{
				fCreationDate = System.currentTimeMillis();
				fUseCRCs = DEFAULT_USE_CRCs;

				long 		headerAddress = fFile.allocate(HEADER_SIZE);
				assert headerAddress == 0;
				io = fFile.getFile();
			}
			else
			{
				io = fFile.getFile();
				io.seek(0);
				if ( io.readInt() != HEADER_VERSION )
				{
					throw new OldFileException("File is an old version and will be ignored: " + fFilePath);
				}
				fUseCRCs = io.readBoolean();
				fCreationDate = io.readLong();
			}

			io.seek(0);

			io.writeInt(HEADER_VERSION);
			io.writeBoolean(DEFAULT_USE_CRCs);
			io.writeLong(fCreationDate);
		}
		finally
		{
			if ( io != null )
			{
				fFile.releaseFile(io);
			}
		}
	}

	private ChunkedByteArray readObject(long address) throws IOException
	{
		CCDB2Record				record = CCDB2Record.existingRecord(fFile, fUseCRCs, address);
		record.load(fDriver, CCDB2Record.LoadMode.ALL);
		if ( !record.CRCsMatch() )
		{
			throw new IOException("crcs don't match at address: " + address);
		}
		return record.getObject();
	}

	private long writeObject(String key, ChunkedByteArray bytes, long[] groupSpecs) throws IOException
	{
		CCDB2Record			record = CCDB2Record.newRecord(fFile, fUseCRCs, key, bytes, groupSpecs);
		record.writeRecord(key, bytes, groupSpecs);
		return record.getAddress();
	}

	static void addToGroup(ConcurrentHashMap<Long, HashSet<String>> map, String key, long groupSpec)
	{
		HashSet<String> 	initialLlist = new HashSet<String>();
		HashSet<String> 	actualList = map.putIfAbsent(groupSpec, initialLlist);
		if ( actualList == null )
		{
			actualList = initialLlist;
		}
		synchronized(actualList)
		{
			actualList.add(key);
		}
	}

	private void updateGroupIndex(String key, long[] groupSpecs)
	{
		if ( groupSpecs != null )
		{
			for ( long spec : groupSpecs )
			{
				addToGroup(fGroupsIndex, key, spec);
			}
		}
	}

	private synchronized void updateUseCount(boolean increment)
	{
		fUseCount += increment ? 1 : -1;
		notifyAll();
	}

	private class PendingPutThread implements Runnable
	{
		@Override
		public void run()
		{
			while ( !Thread.currentThread().isInterrupted() )
			{
				try
				{
					PendingPutRecord 		pendingPut = fPendingPutQueue.take();
					try
					{
						CCDB2IndexEntry 	currentEntry = fIndex.get(pendingPut.key);
						if ( (currentEntry != null) && (currentEntry.address != CCDB2IndexEntry.NOT_EXISTS_ADDRESS) && (currentEntry == pendingPut.entry) )	// otherwise another value was set for the key or the key was removed
						{
							synchronized(pendingPut.entry)
							{
								processPendingPut(pendingPut);
							}
						}
					}
					catch ( IOException e )
					{
						fPendingPutException.set(e);
					}
				}
				catch ( InterruptedException e )
				{
					Thread.currentThread().interrupt();	// restore
					break;
				}
			}
		}
	}

	private static class ActivePendingPut
	{
		final String			key;
		final CCDB2DataSpec 	spec;

		private ActivePendingPut(String key, CCDB2DataSpec spec)
		{
			this.key = key;
			this.spec = spec;
		}
	}

	private static class PendingPutRecord
	{
		final String 				key;
		final ChunkedByteArray		previousBytesRef;
		final CCDB2IndexEntry		entry;
		final CCDB2DataSpec 		spec;
		final long[] 				groupSpecs;
		final boolean 				addToIndexFile;

		@Override
		public boolean equals(Object o)
		{
			if ( this == o )
			{
				return true;
			}
			if ( o == null || getClass() != o.getClass() )
			{
				return false;
			}

			PendingPutRecord that = (PendingPutRecord)o;
			return key.equals(that.key);

		}

		@Override
		public int hashCode()
		{
			return key.hashCode();
		}

		private PendingPutRecord(String key, ChunkedByteArray previousBytesRef, CCDB2IndexEntry entry, CCDB2DataSpec spec, long[] groupSpecs, boolean addToIndexFile)
		{
			if ( entry.address == CCDB2IndexEntry.NOT_EXISTS_ADDRESS )
			{
				entry.address = CCDB2IndexEntry.PENDING_PUT_ADDRESS;
			}

			this.key = key;
			this.previousBytesRef = previousBytesRef;
			this.entry = entry;
			this.spec = spec;
			this.groupSpecs = groupSpecs;
			this.addToIndexFile = addToIndexFile;
		}
	}

	private static final int 		DEAD_BYTE = 0;

	private static final boolean	DEFAULT_USE_CRCs = (System.getProperty("crcs") != null);

	private static final int		HEADER_SIZE = 1024;		// allow room for future growth
	private static final int		HEADER_VERSION = 11;

	private static final long[] 	NULL_GROUP_SPECS = new long[0];

	private final CCDB2Driver										fDriver;
	private	String													fFilePath;
	private volatile CCDB2File										fFile;
	private	long													fCreationDate;
	private	boolean													fUseCRCs;
	private final AtomicBoolean										fIsOpen;
	private	int														fUseCount;
	private final ConcurrentHashMap<String,	CCDB2IndexEntry>		fIndex;
	private final ConcurrentHashMap<Long, HashSet<String>>			fGroupsIndex;
	private final BlockingQueue<PendingPutRecord> 					fPendingPutQueue;
	private final Thread											fPendingPutQueueThread;
	private final AtomicReference<IOException>						fPendingPutException;
	private final AtomicReference<ActivePendingPut>					fActivePendingPut;
	private	final AtomicLong										fIndexSize;
	private	final AtomicLong										fGroupsIndexSize;
	private	final AtomicLong										fInMemoryGetQty;
	private	final AtomicLong										fFromDiskGetQty;
	private	final AtomicLong										fPendingPutQueueOverflowQty;
	private	CCDB2IndexFile											fIndexFile;
}