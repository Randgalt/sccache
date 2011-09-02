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

/**
 * Notifications for {@link GenericIOClientPool}
 *
 * @author Jordan Zimmerman
 */
public interface GenericIOClientPoolListener<T>
{
	/**
	 * Called when a stale (old, unused) client is being closed. Notification can return false to keep the client open
	 *
	 * @param client client about to be closed
	 * @return true to close it, false to keep it open
	 */
	public boolean		staleClientClosing(GenericIOClient<T> client);
}
