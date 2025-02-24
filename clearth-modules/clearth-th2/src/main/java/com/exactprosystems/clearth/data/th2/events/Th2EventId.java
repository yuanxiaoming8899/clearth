/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
 * https://www.exactpro.com
 * Build Software to Test Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactprosystems.clearth.data.th2.events;

import com.exactpro.th2.common.grpc.EventID;
import com.exactprosystems.clearth.data.HandledTestExecutionId;
import com.exactprosystems.clearth.data.th2.serialization.EventIDSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Th2EventId implements HandledTestExecutionId
{
	@JsonSerialize(using = EventIDSerializer.class)  //This is required if action data is saved as JSON
	private final EventID id;
	
	public Th2EventId(EventID id)
	{
		this.id = id;
	}
	
	@Override
	public String toString()
	{
		return id.toString();
	}
	
	public EventID getId()
	{
		return id;
	}
}
