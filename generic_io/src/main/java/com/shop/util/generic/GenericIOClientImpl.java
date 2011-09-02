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

import com.shop.util.LineReader;
import com.shop.util.chunked.ChunkedByteArray;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Internal implementation of the client
 *
 * @author Jordan Zimmerman
 */
class GenericIOClientImpl<T> implements GenericIOClient<T>, GenericIOInputStream.HeartbeatReceivedNotifier
{
	GenericIOClientImpl(Socket s, GenericIOServerImpl<T> parentServer) throws IOException
	{
		fSocket = s;
		fParentServer = parentServer;

		fIOInputStream = new GenericIOInputStream(new BufferedInputStream(fSocket.getInputStream(), fSocket.getReceiveBufferSize()), this);
		fIn = new LineReader(fIOInputStream);
		fOut = new GenericIOOutputStream(new BufferedOutputStream(fSocket.getOutputStream(), fSocket.getSendBufferSize()));
		fUserValue = new AtomicReference<T>(null);
		fLastReadTicks = new AtomicLong(System.currentTimeMillis());
		fLastFlushTicks = new AtomicLong(System.currentTimeMillis());
		fIsOpen = new AtomicBoolean(true);

		fSocket.setTcpNoDelay(true);

		String 		hostname = fSocket.getInetAddress().getHostAddress();
		fAddress = InetSocketAddress.createUnresolved(hostname, s.getPort());

		GenericIOHeartbeatMonitor.instance.addClient(this);
	}

	@Override
	public String toString()
	{
		InetAddress localAddress = fAddress.getAddress();
		return (localAddress != null) ? localAddress.getHostAddress() : fAddress.getHostName();
	}

	@Override
	public void send(String line) throws IOException
	{
		int		length = line.length();
		for ( int i = 0; i < length; ++i )
		{
			char		c = line.charAt(i);
			fOut.write(c & 0xff);
		}
		fOut.write('\n');
	}

	@Override
	public void sendByte(byte b) throws IOException
	{
		fOut.write(b & 0xff);
	}

	@Override
	public void sendBytes(byte[] bytes, int offset, int length) throws IOException
	{
		fOut.write(bytes, offset, length);
	}

	@Override
	public void flush() throws IOException
	{
		fOut.flush();
		fLastFlushTicks.set(System.currentTimeMillis());
	}

	@Override
	public InetSocketAddress getAddress()
	{
		return fAddress;
	}

	@Override
	public int read() throws IOException
	{
		return fIn.read();
	}

	@Override
	public String readLine() throws IOException
	{
		return fIn.readLine();
	}

	@Override
	public ChunkedByteArray readBytes(int size) throws IOException
	{
		boolean					eof = false;
		ChunkedByteArray		bytes = (size < ChunkedByteArray.DEFAULT_CHUNK_SIZE) ? new ChunkedByteArray(size) : new ChunkedByteArray();
		while ( size-- > 0 )
		{
			int		i = fIn.read();
			if ( i < 0 )
			{
				eof = true;
				break;
			}
			bytes.append((byte)(i & 0xff));
			updateLastReadTicks();
		}

		if ( eof )
		{
			if ( bytes.size() == 0 )
			{
				return null;
			}
			throw new EOFException();
		}

		return bytes;
	}

	@Override
	public void disableHeartbeats()
	{
		GenericIOHeartbeatMonitor.instance.removeClient(this);
		fOut.disableEscapes();
		fIOInputStream.disableEscapes();
	}

	@Override
	public void close() throws IOException
	{
		try
		{
			if ( fIsOpen.get() )
			{
				flush();
			}
		}
		finally
		{
			internalClose();
		}
	}

	@Override
	public void setUserValue(T value)
	{
		fUserValue.set(value);
	}

	@Override
	public T getUserValue()
	{
		return fUserValue.get();
	}

	@Override
	public boolean		isOpen()
	{
		return fIsOpen.get();
	}

	@Override
	public void heartbeatReceived()
	{
		updateLastReadTicks();
	}

	@Override
	public GenericIOServer<T> getParentServer()
	{
		return fParentServer;
	}

	void		internalClose()
	{
		if ( !fIsOpen.compareAndSet(true, false) )
		{
			return;
		}

		try
		{
			fSocket.close();
		}
		catch ( IOException e )
		{
			// ignore
		}

		if ( fParentServer != null )
		{
			fParentServer.removeClient(this);
		}

		GenericIOHeartbeatMonitor.instance.removeClient(this);
	}

	void	sendHeartbeat() throws IOException
	{
		flush();
		fOut.writeHeartbeat();
		fLastFlushTicks.set(System.currentTimeMillis());
	}

	long		getLastReadTicks()
	{
		return fLastReadTicks.get();
	}

	long		getLastFlushTicks()
	{
		return fLastFlushTicks.get();
	}

	private void updateLastReadTicks()
	{
		fLastReadTicks.set(System.currentTimeMillis());
	}

	private final Socket 					fSocket;
	private final GenericIOServerImpl<T> 	fParentServer;
	private final InetSocketAddress 		fAddress;
	private final GenericIOInputStream 		fIOInputStream;
	private final LineReader 				fIn;
	private final GenericIOOutputStream		fOut;
	private final AtomicReference<T> 		fUserValue;
	private final AtomicLong 				fLastReadTicks;
	private final AtomicLong				fLastFlushTicks;
	private final AtomicBoolean 			fIsOpen;
}
