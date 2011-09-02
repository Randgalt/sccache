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
package com.shop.util;

import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhances {@link FutureTask} by adding a method to interrupt the task's thread. The JDK version
 * has a cancel method but once that method is called, get() will always throw an exception. It makes it
 * difficult to release partially allocated resources from the internal task. 
 * <br>
 *
 * @author Jordan Zimmerman
 */
public class InterruptibleFutureTask<T> extends FutureTask<T>
{
	/**
	 * {@inheritDoc}
	 * @param c
	 */
	public InterruptibleFutureTask(Callable<T> c)
	{
		super(c);
		fTaskInterrupted = new AtomicBoolean(false);
	}

	/**
	 * @param runnable task
	 * @param result result
	 */
	public InterruptibleFutureTask(Runnable runnable, T result)
	{
		super(runnable, result);
		fTaskInterrupted = new AtomicBoolean(false);
	}

	@Override
	public void run()
	{
		fThread = Thread.currentThread();
		if ( !fTaskInterrupted.get() )
		{
			super.run();
		}
	}

	/**
	 * If {@link #interruptTask()} has been called, null is returned, otherwise the task is executed and the result returned.<br>
	 * @return T
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Override
	public T get() throws InterruptedException, ExecutionException
	{
		return fTaskInterrupted.get() ? null : super.get();
	}

	/**
	 * If {@link #interruptTask()} has been called, null is returned, otherwise the task is executed and the result returned.<br>
	 * {@inheritDoc}
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		return fTaskInterrupted.get() ? null : super.get(timeout, unit);
	}

	/**
	 * Interrupts the thread that the task is running in (if the task has started).
	 */
	public void		interruptTask()
	{
		if ( fTaskInterrupted.compareAndSet(false, true) && (fThread != null) )
		{
			fThread.interrupt();
		}
	}

	private final AtomicBoolean			fTaskInterrupted;
	private volatile Thread 			fThread;
}
