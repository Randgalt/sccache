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
package com.shop.util;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;

/**
 * Factory for making SSL sockets (compatible with HTTPS)<br>
 * This was, essentially, copied from sample code I found
 * at http://groups.google.com/groups?selm=37F1269C.77FA2938%40lutris.com&output=gplain<br>
 * written by steve.latif@lutris.com<br>
 * with some additional stuff found elsewhere<br>
 *
 * @author Jordan Zimmerman
 */
public class SSLSocketMaker
{
	public static class SocketData
	{
		public SocketData(SSLSocketFactory socketFactory, SSLServerSocketFactory serverSocketFactory)
		{
			this.socketFactory = socketFactory;
			this.serverSocketFactory = serverSocketFactory;
		}

		public final SSLSocketFactory 			socketFactory;
		public final SSLServerSocketFactory 	serverSocketFactory;
	}

	/**
	 * Make an SSL client socket using the default factories
	 *
	 * @param address address to connect to
	 * @param port port to connect on
	 * @return the socket
	 * @throws Exception any errors
	 */
	public static SSLSocket		make(String address, int port) throws Exception
	{
		return make(fSocketData, address, port);
	}

	/**
	 * Make an SSL client socket using the given factories
	 *
	 * @param data factories to use
	 * @param address address to connect to
	 * @param port port to connect on
	 * @return the socket
	 * @throws Exception any errors
	 */
	public static SSLSocket		make(SocketData data, String address, int port) throws Exception
	{
		checkException();

		SSLSocket 			socket = (SSLSocket)data.socketFactory.createSocket(address, port);
		socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
		
		return socket;
	}

	/**
	 * Make an SSL server socket using the default factories
	 *
	 * @param port the port to listen on
	 * @param backlog accept backlocg
	 * @return the socket
	 * @throws Exception any errors
	 */
	public static SSLServerSocket makeServer(int port, int backlog) throws Exception
	{
		return makeServer(fSocketData, port, backlog);
	}

	/**
	 * Make an SSL server socket using the given factories
	 *
	 * @param data factories to use
	 * @param port the port to listen on
	 * @param backlog accept backlocg
	 * @return the socket
	 * @throws Exception any errors
	 */
	public static SSLServerSocket makeServer(SocketData data, int port, int backlog) throws Exception
	{
		checkException();

		SSLServerSocket 		server_socket = (SSLServerSocket)data.serverSocketFactory.createServerSocket(port, backlog);
		server_socket.setEnabledCipherSuites(server_socket.getSupportedCipherSuites());
		
		return server_socket;
	}

	/**
	 * Create server/socket factories for the given keystore
	 *
	 * @param keystore_path path to the keystore
	 * @param keystore_password the keystore's password
	 * @return the factories
	 * @throws Exception on error
	 */
	public static SocketData makeFactories(File keystore_path, String keystore_password) throws Exception
	{
		return makeFactories(keystore_path, keystore_password, KEYSTORE_TYPE);
	}

	/**
	 * Create server/socket factories for the given keystore
	 *
	 * @param keystorePath path to the keystore
	 * @param keystorePassword the keystore's password
	 * @param keystoreType the keystore type (e.g. "PKCS12")
	 * @return the factories
	 * @throws Exception on error
	 */
	public static SocketData makeFactories(File keystorePath, String keystorePassword, String keystoreType) throws Exception
	{
		return makeFactories(initKeystore(keystorePath, keystorePassword, keystoreType), keystorePassword);
	}

	/**
	 * Create server/socket factories for the given keystore
	 *
	 * @param keystore the keystore to use or null
	 * @param keystorePassword the keystore's password (ignored if keystore is null)
	 * @return the factories
	 * @throws Exception on error
	 */
	public static SocketData makeFactories(KeyStore keystore, String keystorePassword) throws Exception
	{
		/*
			This code was discovered through painful trial and error searching the net. -JZ
		 */

		SecureRandom 			secureRandom = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM, SECURE_RANDOM_PROVIDER);

		// Interestingly this will not work with the default provider SUN hmmmm ...
		// The refernce implementation only supports the protocol TLS or SSL
		SSLContext 				sslContext = SSLContext.getInstance(SSL_CONTEXT_PROTOCOL);

		KeyManagerFactory 		keyManagerFactory = KeyManagerFactory.getInstance(SUN_X509);
		keyManagerFactory.init(keystore, keystorePassword.toCharArray());
		KeyManager[] 			keyManager = keyManagerFactory.getKeyManagers();

		sslContext.init(keyManager, null, secureRandom);

		SSLSocketFactory 		socketFactory = sslContext.getSocketFactory();
		SSLServerSocketFactory 	serverSocketFactory = sslContext.getServerSocketFactory();

		return new SocketData(socketFactory, serverSocketFactory);
	}

	/**
	 * Initialize and return a keystore
	 *
	 * @param keystorePath path to the keystore
	 * @param keystorePassword keystore's password
	 * @param keystoreType keystore type
	 * @return the keystore
	 * @throws KeyStoreException errors
	 * @throws IOException errors
	 * @throws NoSuchAlgorithmException errors
	 * @throws CertificateException errors
	 */
	public static KeyStore initKeystore(File keystorePath, String keystorePassword, String keystoreType) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
	{
		if ( keystorePath != null )
		{
			InputStream			in = new BufferedInputStream(new FileInputStream(keystorePath));
			try
			{
				return initKeystore(in, keystorePassword, keystoreType);
			}
			finally
			{
				in.close();
			}
		}
		return null;
	}

	/**
	 * Initialize and return a keystore
	 *
	 * @param keystoreStream keystore
	 * @param keystorePassword keystore's password
	 * @param keystoreType keystore type
	 * @return the keystore
	 * @throws KeyStoreException errors
	 * @throws IOException errors
	 * @throws NoSuchAlgorithmException errors
	 * @throws CertificateException errors
	 */
	public static KeyStore initKeystore(InputStream keystoreStream, String keystorePassword, String keystoreType) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
	{
		KeyStore		keystore = KeyStore.getInstance(keystoreType);
		keystore.load(keystoreStream, keystorePassword.toCharArray());
		return keystore;
	}

	private static void checkException() throws Exception
	{
		if ( fInitException != null )
		{
			throw fInitException;
		}
	}

	private SSLSocketMaker()
	{
	}

	private	static final String			SECURE_RANDOM_ALGORITHM	= "SHA1PRNG";
	private	static final String			SECURE_RANDOM_PROVIDER = "SUN";
	private	static final String			SSL_CONTEXT_PROTOCOL = "TLS";
	private static final String 		KEYSTORE_TYPE = "JKS";
	private	static final String			SUN_X509 = "SunX509";

	private	static final String 		DEFAULT_KEYSTORE_PASSWORD =	"password";

	static
	{
		System.setProperty("java.protocol.handler.pkgs", "javax.net.ssl");
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

		SocketData data = null;
		try
		{
			data = makeFactories((KeyStore)null, DEFAULT_KEYSTORE_PASSWORD);
		}
		catch ( Exception e )
		{
			fInitException = e;
		}
		fSocketData = data;
	}

	private static final SocketData			fSocketData;
	private static volatile Exception 		fInitException;
}

/*
 * History:$
 */

