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

import com.shop.cache.api.server.SCServerContext;
import java.io.File;

/**
 * SHOP.COM's Server Context implementation
 *
 * @author Jordan Zimmerman
 */
class ImpSCServerContext implements SCServerContext
{
	ImpSCServerContext()
	{
		fPort = fMonitorPort = 0;
		fLogPath = null;
	}

	@Override
	public int getPort()
	{
		return fPort;
	}

	@Override
	public SCServerContext port(int port)
	{
		this.fPort = port;
		return this;
	}

	@Override
	public int getMonitorPort()
	{
		return fMonitorPort;
	}

	@Override
	public SCServerContext monitorPort(int monitorPort)
	{
		this.fMonitorPort = monitorPort;
		return this;
	}

	@Override
	public File getLogPath()
	{
		return (fLogPath != null) ? new File(fLogPath.getPath()) : null;
	}

	@Override
	public SCServerContext logPath(File logPath)
	{
		this.fLogPath = (logPath != null) ? new File(logPath.getPath()) : null;
		return this;
	}

	private int					fPort;
	private int					fMonitorPort;
	private File				fLogPath;
}
