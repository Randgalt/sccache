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

import com.shop.util.ccdb2.CCDB2Instance;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Used when loading/reading DB files
 *
 * @author Jordan Zimmerman
 */
class CCDB2InstanceLoader
{
	interface ProcessDriver
	{
		public void			process(CCDB2Instance instance, AtomicInteger percentDone) throws IOException;
	}

	CCDB2InstanceLoader(CCDB2Storage db, List<CCDB2Instance> instances, ProcessDriver driver)
	{
		fDB = db;
		fDataList = new ArrayList<EntryData>();
		for ( CCDB2Instance instance : instances )
		{
			EntryData data = new EntryData();
			data.instance = instance;
			data.thread = new Thread(data);
			data.exception = null;
			data.percentDone = new AtomicInteger(0);
			data.driver = driver;

			fDataList.add(data);
		}
		fRunningCount = 0;
	}

	void			load() throws IOException
	{
		synchronized(this)
		{
			fRunningCount = fDataList.size();
		}
		for ( EntryData data : fDataList )
		{
			data.thread.start();
		}

		long 			startTicks = System.currentTimeMillis();
		boolean 		firstTime = true;
		synchronized(this)
		{
			while ( fRunningCount > 0 )
			{
				try
				{
					wait(UPDATE_TICKS);
				}
				catch ( InterruptedException dummy )
				{
					break;
				}

				outputPercentDone(firstTime);
				firstTime = false;
			}
		}
		fDB.log((System.currentTimeMillis() - startTicks) + " ms", null, true);

		for ( EntryData data : fDataList )
		{
			if ( data.exception != null )
			{
				throw data.exception;
			}
		}
	}

	private void outputPercentDone(boolean firstTime)
	{
		String[] 		headingList = new String[fDataList.size()];
		String[] 		displayList = new String[fDataList.size()];
		int 			maxHeaderLength = 0;
		for ( int i = 0; i < fDataList.size(); ++i )
		{
			EntryData data = fDataList.get(i);
			headingList[i] = (new File(data.instance.getDatabaseName())).getName();
			displayList[i] = data.percentDone.get() + "%";

			if ( headingList[i].length() > maxHeaderLength )
			{
				maxHeaderLength = headingList[i].length();
			}
		}

		maxHeaderLength += 3;	// margin

		if ( firstTime )
		{
			for ( String s : headingList )
			{
				fDB.log(s, null, false);
				for ( int i = 0; i < (maxHeaderLength - s.length()); ++i )
				{
					fDB.log(" ", null, false);
				}
			}
			fDB.log(System.getProperty("line.separator"), null, false);
		}
		for ( String s : displayList )
		{
			fDB.log(s, null, false);
			for ( int i = 0; i < (maxHeaderLength - s.length()); ++i )
			{
				fDB.log(" ", null, false);
			}
		}
		fDB.log(System.getProperty("line.separator"), null, false);
	}

	private class EntryData implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				driver.process(instance, percentDone);
			}
			catch ( IOException e )
			{
				exception = e;
			}
			synchronized(CCDB2InstanceLoader.this)
			{
				--fRunningCount;
				CCDB2InstanceLoader.this.notifyAll();
			}
		}

		CCDB2Instance		instance;
		Thread				thread;
		IOException			exception;
		AtomicInteger percentDone;
		ProcessDriver driver;
	}

	private static final int		UPDATE_TICKS = 5000;

	private final CCDB2Storage fDB;
	private final List<EntryData> 	fDataList;
	private int 					fRunningCount;
}
