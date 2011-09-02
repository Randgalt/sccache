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
package com.shop.cache.api.commands;

import java.util.List;

/**
 * Interface for each cache command
 *
 * @author Jordan Zimmerman
 */
public interface SCCommand
{
	/**
	 * Returns the arguments expected by the command
	 *
	 * @return argument list
	 */
	public List<SCDataBuilderTypeAndCount>	getTypesAndCounts();

	/**
	 * Return a new data builder for this command
	 *
	 * @return builder
	 */
	public SCDataBuilder					newBuilder();

	/**
	 * Return true if this command should be available for monitor mode
	 *
	 * @return true/false
	 */
	public boolean 							isMonitorCommand();
}
