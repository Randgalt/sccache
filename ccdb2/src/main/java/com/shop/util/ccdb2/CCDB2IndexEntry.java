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

import com.shop.util.chunked.ChunkedByteArray;
import java.lang.ref.SoftReference;

/**
 * An index entry in memory<br>
 *
 * @author Jordan Zimmerman
*/
class CCDB2IndexEntry
{
	/**
	 * Approximate size of this record - i.e. if this were a C struct
	 */
	static final int INDEX_BASE_SIZE = 8 + 4 + 4;

	static final int MINIMUM_ACTIVE_ADDRESS = 0;
	static final int NOT_EXISTS_ADDRESS = -1;
	static final int PENDING_PUT_ADDRESS = -2;

	/**
	 * Address in the DB of the object
	 */
	long								address	= NOT_EXISTS_ADDRESS;

	/**
	 * Purgeable reference to the data
	 */
	SoftReference<ChunkedByteArray>		bytesRef = null;

	/**
	 * Object's TTL
	 */
	int									TTLDelta = 0;
}