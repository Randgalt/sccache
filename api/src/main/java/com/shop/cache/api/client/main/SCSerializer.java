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
package com.shop.cache.api.client.main;

import com.shop.util.chunked.ChunkedByteArray;

/**
 * Interface for serializing/deserializing objects
 *
 * @author Jordan Zimmerman
 */
public interface SCSerializer
{
	/**
	 * Serialize the given block
	 *
	 * @param block block containing the object
	 * @return serialized version
	 * @throws Exception errors
	 */
	public ChunkedByteArray	serialize(SCDataBlock block) throws Exception;

	/**
	 * Deserialize the given bytes
	 *
	 * @param bytes the bytes (as they were returned from {@link #serialize(SCDataBlock)}
	 * @return the block
	 * @throws Exception errors
	 */
	public SCDataBlock 		deserialize(ChunkedByteArray bytes) throws Exception;
}
