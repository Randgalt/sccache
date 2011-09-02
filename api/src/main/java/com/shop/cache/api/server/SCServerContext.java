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
package com.shop.cache.api.server;

import java.io.File;

/**
 * Builder interface for containing server data. NOTE: it is not necessary to set every
 * value. Implementations should provide reasonable defaults where possible
 *
 * @author Jordan Zimmerman
 */
public interface SCServerContext
{
	/**
	 * Port for main server to listen on
	 *
	 * @param port the port
	 * @return this
	 */
	public SCServerContext port(int port);

	/**
	 * Port for monitor server to listen on. This is optional. The monitor server
	 * helps in detecting server failures. If the main server fails (out of disk space, etc.)
	 * it will close. However, the monitor server will stay active and will report that the main server
	 * has gone down.
	 *
	 * @param monitorPort the port or 0 for no monitor server
	 * @return this
	 */
	public SCServerContext monitorPort(int monitorPort);

	/**
	 * Sets a path to a log file.
	 *
	 * @param logPath the path or null for no logging
	 * @return this
	 */
	public SCServerContext logPath(File logPath);

	/**
	 * Returns the main server port
	 *
	 * @return port
	 */
	public int 		getPort();

	/**
	 * Returns the monitor server port
	 *
	 * @return port or 0
	 */
	public int 		getMonitorPort();

	/**
	 * Returns the log file path
	 *
	 * @return path or null
	 */
	public File 	getLogPath();
}
