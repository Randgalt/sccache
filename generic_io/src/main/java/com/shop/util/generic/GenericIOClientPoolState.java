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

package com.shop.util.generic;

import com.shop.util.InterruptibleFutureTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * State for the generic client/server pool - must be atomic and there are 2 values that make up the state.<br>
 *
 * @author Jordan Zimmerman
 */
class GenericIOClientPoolState<T>
{
	/**
	 * Init the state to open
	 */
	GenericIOClientPoolState()
	{
		fReopener = null;
		fIsOpen = true;
	}

	/**
	 * Returns true if the state is set to open
	 *
	 * @return true/false
	 */
	synchronized boolean 	isOpen()
	{
		return fIsOpen;
	}

	/**
	 * Returns true if the state is set to open and there is no current reopener
	 *
	 * @return true/false
	 */
	synchronized boolean isOpenAndNoReopener()
	{
		return fIsOpen && (fReopener == null);
	}

	/**
	 * Sets the state to closed and returns the current reopener task if any. The reopener task
	 * will be internally cleared. The caller is responsible for managing the returned task.
	 *
	 * @return the current reopener task or null
	 */
	synchronized InterruptibleFutureTask<GenericIOClient<T>> close()
	{
		InterruptibleFutureTask<GenericIOClient<T>> 	localReopener = fReopener;

		fReopener = null;
		fIsOpen = false;
		return localReopener;
	}

	/**
	 * Sets the reopener task unless there is already any active one. If the given reopener is successfully
	 * set, true is returned. Otherwise false is returned and the caller is responsible for managing the returned task.
	 *
	 * @param newReopener new reopener task
	 * @return true/false
	 */
	synchronized boolean checkSetReopener(InterruptibleFutureTask<GenericIOClient<T>> newReopener)
	{
		if ( fIsOpen && (fReopener == null) )
		{
			fReopener = newReopener;
			return true;
		}
		return false;
	}

	/**
	 * Atomically checks and updates the state. If there is a current reopener, it is returned. If the reopener task is
	 * also done, the internal reopener state is cleared. Additionally, the current is-open state is returned
	 *
	 * @param isOpen the open/closed state is returned here
	 * @return the current reopener (which may not yet be done) or null
	 */
	synchronized InterruptibleFutureTask<GenericIOClient<T>> getAndCheckDoneReopener(AtomicReference<Boolean> isOpen)
	{
		isOpen.set(fIsOpen);

		InterruptibleFutureTask<GenericIOClient<T>> 	localReopener = fReopener;
		if ( (localReopener != null) && localReopener.isDone() )
		{
			fReopener = null;
		}
		return localReopener;
	}

	private InterruptibleFutureTask<GenericIOClient<T>> 		fReopener;
	private boolean 											fIsOpen;
}