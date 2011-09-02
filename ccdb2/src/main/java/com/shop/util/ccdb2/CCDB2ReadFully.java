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

/**
 * <br>
 *
 * @author Jordan Zimmerman
 */
public interface CCDB2ReadFully
{
	/**
	 * Used internally to read a buffer
	 *
	 * @param buffer to read
	 * @throws IOException errors
	 */
	public void readFully(byte[] buffer) throws IOException;
}