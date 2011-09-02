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

import com.shop.util.chunked.ChunkedByteArray;
import com.shop.cache.api.server.SCConnection;
import com.shop.cache.api.server.SCServer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Jordan Zimmerman
 */
@SCDoc
(
	description =
		"Supports a ping from an HTTP-type browser. Returns the word \"1 [tab] OK\" - " +
		"properly formatted as an HTTP response. If there is an error state, \"0 [tab] [message]\" is returned. " +
		"NOTE: the connection is closed after the response is returned",
	parameters = {}
)
public class SCCommandHTTPPing implements SCCommand
{
	SCCommandHTTPPing()
	{
	}

	@Override
	public List<SCDataBuilderTypeAndCount> getTypesAndCounts()
	{
		return fTypesAndCounts;
	}

	@Override
	public SCDataBuilder newBuilder()
	{
		return new SCDataBuilder()
		{
			@Override
			public void addNextValue(String value)
			{
			}

			@Override
			public void addNextObject(ChunkedByteArray o)
			{
			}

			@Override
			public void executeCommand(SCServer server, SCConnection connection) throws IOException
			{
				String 		content = getStatus(server);

				connection.sendValue("HTTP/1.1 200 OK");
				connection.sendValue("Content-Type: text/plain");
				connection.sendValue("Content-Length: " + content.length());
				connection.sendValue("Connection: close");
				connection.sendValue("Cache-Control: no-cache");
				connection.sendValue("Expires: Thu, 01 Jan 1970 00:00:00 GMT");
				connection.sendValue("Pragma: no-cache");
				connection.sendValue("");
				connection.sendValue(content);

				connection.close();
			}
		};
	}

	@Override
	public boolean isMonitorCommand()
	{
		return true;
	}

	private String getStatus(SCServer server)
	{
		String		error = server.getErrorState();
		return (error != null) ? ("0\t" + error) : "1\tOK";
	}

	private static final List<SCDataBuilderTypeAndCount>		fTypesAndCounts = Collections.unmodifiableList
	(
		Arrays.asList
		(
			new SCDataBuilderTypeAndCount(SCDataBuilderTypes.UNBOUNDED_VALUE_SET)
		)
	);
}