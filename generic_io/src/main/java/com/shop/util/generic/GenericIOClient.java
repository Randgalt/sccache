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

import com.shop.util.chunked.ChunkedByteArray;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Client/connection abstraction. Use {@link GenericIOFactory} to allocate a new connection. 
 *
 * @author Jordan Zimmerman
 */
public interface GenericIOClient<T>
{
	/**
	 * Send a line of text. NOTE 1: clients maintain a write buffer. Use {@link #flush()} to flush the buffer.
	 * NOTE 2: This method may block if the underlying socket's write buffer is full
	 *
	 * @param line the line to send
	 * @throws IOException errors
	 */
	public void					send(String line) throws IOException;

	/**
	 * Send a single byte. NOTE 1: clients maintain a write buffer. Use {@link #flush()} to flush the buffer.
	 * NOTE 2: This method may block if the underlying socket's write buffer is full
	 *
	 * @param b the byte to send
	 * @throws IOException errors
	 */
	public void 				sendByte(byte b) throws IOException;

	/**
	 * Send bytes. NOTE 1: clients maintain a write buffer. Use {@link #flush()} to flush the buffer.
	 * NOTE 2: This method may block if the underlying socket's write buffer is full
	 *
	 * @param bytes the bytes to send
	 * @param offset offset within the bytes to start at
	 * @param length number of bytes to send
	 * @throws IOException errors
	 */
	public void 				sendBytes(byte[] bytes, int offset, int length) throws IOException;

	/**
	 * Flush any pending writes through. NOTE: This method may block if the underlying socket's write buffer is full
	 *
	 * @throws IOException errors
	 */
	public void					flush() throws IOException;

	/**
	 * Read a single byte. NOTE: this will block until a byte is available to be read. -1 is returned
	 * if the connection closes.
	 *
	 * @return the byte value or -1
	 * @throws IOException errors
	 */
	public int 					read() throws IOException;

	/**
	 * Read a line of text. NOTE: this will block until a line is available to be read. null is returned
	 * if the connection closes.
	 *
	 * @return the line or null
	 * @throws IOException errors
	 */
	public String 				readLine() throws IOException;

	/**
	 * Read the given number of bytes. NOTE: this will block until the asked for number of bytes are available to be read. null is returned
	 * if the connection closes.
	 *
	 * @param size number of bytes to read
	 * @return the bytes or null
	 * @throws IOException errors
	 */
	public ChunkedByteArray 	readBytes(int size) throws IOException;

	/**
	 * Return the address this client is connected to
	 *
	 * @return address
	 */
	public InetSocketAddress	getAddress();

	/**
	 * Returns true if the connection is still open
	 *
	 * @return true/false
	 */
	public boolean				isOpen();

	/**
	 * Close the client - any currently blocking operations will exit with an exception
	 *
	 * @throws IOException errors
	 */
	public void					close() throws IOException;

	/**
	 * Associate a custom value with this server. Use {@link #getUserValue()} to retrieve the value
	 *
	 * @param value the value
	 */
	public void 				setUserValue(T value);

	/**
	 * Returns the value set via {@link #setUserValue(Object)}
	 *
	 * @return value
	 */
	public T 					getUserValue();

	/**
	 * By default, GenericIO clients use a heartbeat mechanism to
	 * validate server-to-client and client-to-server connections. Calling this
	 * method turns this feature off for this connection.<p>
	 *
	 * Use this with caution. It stops the client from sending hearbeats and also
	 * stops the client from <b>interpreting</b> heartbeats.
	 */
	public void 				disableHeartbeats();

	/**
	 * Return the server that this client is part of or null if it's a direct client connection
	 *
	 * @return server or null
	 */
	public GenericIOServer<T> 	getParentServer();
}
