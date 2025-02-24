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

package com.exactprosystems.clearth.web.misc;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ProcessedConnectionsCache
{
	private List<ClearThConnection> cached = null,
			processed = null;
	
	public List<ClearThConnection> refreshIfNeeded(List<ClearThConnection> currentConnections)
	{
		if (isNeedRefresh(currentConnections))
			refresh(currentConnections);
		return getConnections();
	}
	
	public List<ClearThConnection> getConnections()
	{
		return processed;
	}
	
	
	protected abstract List<ClearThConnection> processConnections(List<ClearThConnection> connections);
	
	
	protected boolean isNeedRefresh(List<ClearThConnection> currentConnections)
	{
		return cached == null || !cached.equals(currentConnections);
	}
	
	
	private void refresh(List<ClearThConnection> currentConnections)
	{
		List<ClearThConnection> toStore = new ArrayList<>(currentConnections);
		cached = Collections.unmodifiableList(toStore);
		processed = Collections.unmodifiableList(processConnections(toStore));
	}
}