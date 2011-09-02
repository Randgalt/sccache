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

import java.util.List;

/**
 * Timing class with stats
 *
 * @author Jordan Zimmerman
 */
class TrackerTimer
{
	static class data
	{
		public data(String label)
		{
			fLabel = label;
			fBuckets = new int[BUCKET_QTY];
			fBucketLabels = new String[BUCKET_QTY];
		}

		private synchronized data deepClone()
		{
			data		copy = new data("");

			copy.fLabel = fLabel;
            copy.fMinTime = fMinTime;
            copy.fMaxTime = fMaxTime;
			copy.fMinTimeLabel = fMinTimeLabel;
			copy.fMaxTimeLabel = fMaxTimeLabel;

			copy.fBuckets = new int[fBuckets.length];
			System.arraycopy(fBuckets, 0, copy.fBuckets, 0, fBuckets.length);

			fBucketLabels = new String[fBucketLabels.length];
			System.arraycopy(fBucketLabels, 0, copy.fBucketLabels, 0, fBucketLabels.length);

			return copy;
		}

		private int[] 			fBuckets;
		private String[] 		fBucketLabels;
		private String 			fLabel;
		private int 			fMinTime = Integer.MAX_VALUE;
		private int 			fMaxTime = 0;
		private String 			fMinTimeLabel = "";
		private String 			fMaxTimeLabel = "";
	}

	TrackerTimer(data d)
	{
		fData = d;
		fTicks = 0;
	}

	void		start()
	{
		fTicks = System.currentTimeMillis();
	}

	int		end(String label)
	{
		int				time = (int)(System.currentTimeMillis() - fTicks);
		int				which_bucket = (time / 1000);
		if ( which_bucket >= BUCKET_QTY )
		{
			which_bucket = BUCKET_QTY - 1;
		}
		else if ( which_bucket < 0 )
		{
			which_bucket = 0;
		}

		synchronized(fData)
		{
			if ( (time > 0) && (time < fData.fMinTime) )
			{
				fData.fMinTime = time;
				fData.fMinTimeLabel = label;
			}

			if ( time > fData.fMaxTime )
			{
				fData.fMaxTime = time;
				fData.fMaxTimeLabel = label;
			}

			++fData.fBuckets[which_bucket];
			fData.fBucketLabels[which_bucket] = label;
		}

		return time;
	}

	static void		output(List<String> tab, data theData, boolean complete)
	{
		data 		localData = theData.deepClone();

		tab.add(localData.fLabel + " min time: " + fixTime(localData.fMinTime) + " ms - from " + localData.fMinTimeLabel);
		tab.add(localData.fLabel + " max time: " + localData.fMaxTime + " ms - from " + localData.fMaxTimeLabel);
		if ( complete )
		{
			for ( int i = 0; i < BUCKET_QTY; ++i )
			{
				StringBuilder		str = new StringBuilder();
				str.append(localData.fLabel);
				if ( (i + 1) < BUCKET_QTY )
				{
					str.append(" times < ").append(i + 1);
				}
				else
				{
					str.append(" times >= " + BUCKET_QTY);
				}
				str.append(" secs: ").append(localData.fBuckets[i]);
				if ( localData.fBucketLabels[i] != null )
				{
					str.append(", last from ").append(localData.fBucketLabels[i]);
				}

				tab.add(str.toString());
			}
		}
	}

	private static int fixTime(int time)
	{
		return (time == Integer.MAX_VALUE) ? 0 : time;
	}

	private static final int		BUCKET_QTY = 100;

	private final data 	fData;
	private long 		fTicks;
}
