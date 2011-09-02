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

import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A file pool for multi-threaded access to a single file<br>
 *
 * @author Jordan Zimmerman
 */
class CCDB2FilePool
{
	/**
	 * @param driver the driver
	 * @param f the file to manage
	 */
	CCDB2FilePool(CCDB2Driver driver, File f)
	{
		fFile = f;
		fDriver = driver;
		fQueue = new LinkedBlockingQueue<RandomAccessFile>(INITIAL_QUEUE_LENGTH);
		fInUseCount = new AtomicInteger(0);
		fMaxInUseCount = 0;
		fLastUpdateTicks = 0;
	}

	/**
	 * Returns the current number of users of the file
	 *
	 * @return user qty
	 */
	int 		getTotalQty()
	{
		return fInUseCount.get() + fQueue.size();
	}

	/**
	 * Close the file
	 */
	void			close()
	{
		for ( RandomAccessFile file; (file = fQueue.poll()) != null ; /* no inc */ )
		{
			try
			{
				file.close();
			}
			catch ( IOException e )
			{
				fDriver.handleException(e);
			}
		}
	}

	/**
	 * Get a new/cached file handle. Each call to get() must be balanced by a call to {@link #release(RandomAccessFile)}
	 *
	 * @return the file handle
	 * @throws FileNotFoundException errors
	 */
	RandomAccessFile		get() throws FileNotFoundException
	{
		int 		newCount = fInUseCount.incrementAndGet();
		if ( newCount > fMaxInUseCount )
		{
			fMaxInUseCount = newCount;	// this isn't thread safe. But, it's not a critical value so that's OK
		}
		checkUpdate();

		RandomAccessFile		file = fQueue.poll();
		if ( file == null )
		{
			file = new RandomAccessFile(fFile, "rw");
		}
		return file;
	}

	/**
	 * Call to release a file handle. IMPORTANT: if you don't release the file, you will have a resource leak.
	 *
	 * @param file file handle as returned by {@link #get()} 
	 */
	void		release(RandomAccessFile file)
	{
		fInUseCount.decrementAndGet();

		internalRelease(file);
	}

	private void internalRelease(RandomAccessFile file)
	{
		if ( !fQueue.offer(file) )
		{
			try
			{
				file.close();
			}
			catch ( IOException e )
			{
				fDriver.handleException(e);
			}
		}
	}

	private void checkUpdate()
	{
		LinkedBlockingQueue<RandomAccessFile> 	oldQueue = null;
		synchronized(this)
		{
			if ( (System.currentTimeMillis() - fLastUpdateTicks) > UPDATE_TICKS )
			{
				fLastUpdateTicks = System.currentTimeMillis();

				int proposedQueueSize = ((fMaxInUseCount + (INITIAL_QUEUE_LENGTH - 1)) / INITIAL_QUEUE_LENGTH) * INITIAL_QUEUE_LENGTH;
				if ( proposedQueueSize != fQueue.size() )
				{
					oldQueue = fQueue;
					fQueue = new LinkedBlockingQueue<RandomAccessFile>(proposedQueueSize);
				}

				fMaxInUseCount = INITIAL_QUEUE_LENGTH;
			}
		}

		if ( oldQueue != null )
		{
			for ( RandomAccessFile q : oldQueue )
			{
				internalRelease(q);
			}
		}
	}

	private static final int INITIAL_QUEUE_LENGTH = 10;

	private static final long UPDATE_TICKS = 5 * 60 * 1000;	// 5 minutes

	private final File											fFile;
	private final CCDB2Driver									fDriver;
	private final AtomicInteger									fInUseCount;
	private volatile LinkedBlockingQueue<RandomAccessFile>		fQueue;
	private volatile long										fLastUpdateTicks;
	private volatile int										fMaxInUseCount;
}