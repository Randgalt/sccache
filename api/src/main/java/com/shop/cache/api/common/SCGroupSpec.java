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
package com.shop.cache.api.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

/**
 * Abstraction for a set of groups
 *
 * @author Jordan Zimmerman
 */
public class SCGroupSpec implements Iterable<SCGroup>
{
	public SCGroupSpec(List<SCGroup> groups)
	{
		fList = Collections.unmodifiableList(new ArrayList<SCGroup>(groups));
	}
	
	public SCGroupSpec(SCGroup... groups)
	{
		List<SCGroup>		work = new ArrayList<SCGroup>();
		if ( groups != null )
		{
			work.addAll(Arrays.asList(groups));
		}
		fList = Collections.unmodifiableList(work);
	}

	public int			size()
	{
		return fList.size();
	}

	public SCGroup 		get(int index)
	{
		return fList.get(index);
	}

	@Override
	public Iterator<SCGroup> iterator()
	{
		final Iterator<SCGroup> 	iterator = fList.iterator();
		return new Iterator<SCGroup>()
		{
			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public SCGroup next()
			{
				return iterator.next();
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	private final List<SCGroup>		fList;
}
