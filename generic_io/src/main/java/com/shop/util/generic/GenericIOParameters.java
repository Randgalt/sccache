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
 * Common parameters for making servers and clients 
 *
 * @author Jordan Zimmerman
 */
public class GenericIOParameters
{
	public GenericIOParameters()
	{
		fPort = 0;
		fSSL = false;
	}

	public GenericIOParameters port(int port)
	{
		this.fPort = port;
		return this;
	}

	public GenericIOParameters ssl(boolean ssl)
	{
		this.fSSL = ssl;
		return this;
	}

	int getPort()
	{
		return fPort;
	}

	boolean getSSL()
	{
		return fSSL;
	}

	private int 		fPort;
	private boolean 	fSSL;
}
