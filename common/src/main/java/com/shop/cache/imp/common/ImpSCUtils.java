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
package com.shop.cache.imp.common;

import com.shop.cache.api.server.SCLoggable;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedByInterruptException;

/**
 * Various utilities
 *
 * @author Jordan Zimmerman
 */
public class ImpSCUtils
{
	/**
	 * idle ticks used
	 */
	public static final int 	IDLE_NOTIFICATION_TICKS = 30000;

	/**
	 * Logs the given exception
	 *
	 * @param e exception
	 * @param logger logger
	 * @return true if it was an ignorable exception
	 */
	public static boolean		handleException(Throwable e, SCLoggable logger)
	{
		if ( e instanceof SocketTimeoutException )
		{
			return true;
		}

		if ( isCloseConnectionException(e) )
		{
			return true;
		}

		if ( logger != null )
		{
			logger.log("Exception", e, true);
		}

		return false;
	}

	/**
	 * Returns true if the given exception is one of the many types that represent a closed connection of some kind
	 *
	 * @param e exception to check
	 * @return true/false
	 */
	public static boolean isCloseConnectionException(Throwable e)
	{
		if ( e instanceof ImpSCClosedConnectionException )
		{
			return true;
		}

		String 		message = e.getMessage();
		String 		lowercaseMessage = (message != null) ? message.toLowerCase() : "";
		if ( lowercaseMessage.indexOf("socket closed") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("socket write error") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("has closed") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("not a socket") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("forcibly closed") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("reset") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("recv failed") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("connection was aborted") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("10054") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("10053") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("10058") >= 0 )
		{
			// ignore these
			return true;
		}

		if ( lowercaseMessage.indexOf("code = 64") >= 0 )
		{
			// ignore these
			return true;
		}

		//noinspection RedundantIfStatement
		if ( e instanceof ClosedByInterruptException )
		{
			// ignore
			return true;
		}

		return false;
	}

	private ImpSCUtils()
	{		
	}
}
