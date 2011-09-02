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
package com.shop.util.ccdb2;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Abstraction for I/O APIs<br>
 *
 * @author Jordan Zimmerman
 */
public interface CCDB2io extends CCDB2ReadFully
{
	/**
	 * Seek to the given offset
	 *
	 * @param i offset
	 * @throws IOException errors
	 */
	public void seek(long i) throws IOException;

	/**
	 * Write a byte at the current offset
	 *
	 * @param b byte
	 * @throws IOException error
	 */
	public void writeByte(byte b) throws IOException;

	/**
	 * Write an int at the current offset
	 *
	 * @param i int
	 * @throws IOException error
	 */
	public void writeInt(int i) throws IOException;

	/**
	 * Write a boolean at the current offset
	 *
	 * @param b boolean
	 * @throws IOException error
	 */
	public void writeBoolean(boolean b) throws IOException;

	/**
	 * Write a long at the current offset
	 *
	 * @param l long
	 * @throws IOException error
	 */
	public void writeLong(long l) throws IOException;

	/**
	 * Read a byte from the current offset
	 *
	 * @return the byte
	 * @throws IOException errors
	 */
	public byte readByte() throws IOException;

	/**
	 * Read a int from the current offset
	 *
	 * @return the int
	 * @throws IOException errors
	 */
	public int readInt() throws IOException;

	/**
	 * Read a boolean from the current offset
	 *
	 * @return the boolean
	 * @throws IOException errors
	 */
	public boolean readBoolean() throws IOException;

	/**
	 * Read a long from the current offset
	 *
	 * @return the long
	 * @throws IOException errors
	 */
	public long readLong() throws IOException;

	/**
	 * Write out bytes at the current offset
	 *
	 * @param bytes the bytes
	 * @throws IOException errors
	 */
	public void write(byte[] bytes) throws IOException;

	/**
	 * Write out bytes at the current offset
	 *
	 * @param bytes the bytes
	 * @param length number of bytes to write
	 * @throws IOException errors
	 */
	public void write(byte[] bytes, int length) throws IOException;

	@Override
	public void readFully(byte[] buffer) throws IOException;

	/**
	 * Returns the Java file object
	 *
	 * @return file
	 */
	public RandomAccessFile getUnderlyingFile();
}