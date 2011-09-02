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

/**
 * Interface for receiving interesting event notifications. The notifications
 * are for informational purposes only.
 *
 * @author Jordan Zimmerman
 */
public interface SCNotifications
{
	/**
	 * Called when an exception is caught internally. Exceptions are always handled in a well defined manner
	 * but you may want to print a diagnostic or post an alert (especially if the error is out of disk space, etc.).
	 *
	 * @param message Diagnostic message
	 * @param e the exception
	 */
	public void 		notifyException(String message, Throwable e);

	/**
	 * Called when the SCMultiManager uses a client
	 *
	 * @param clientName name of the client being used (usually it's address)
	 */
	public void 		notifyClientAccess(String clientName);

	/**
	 * Called by the SCClientManager when a server goes down
	 *
	 * @param name name of the server (usually it's address)
	 */
	public void 		notifyServerDown(String name);
}
