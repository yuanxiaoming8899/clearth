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

package com.exactprosystems.clearth.connectivity.connections;

import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.exactprosystems.clearth.connectivity.MessageListener;
import com.exactprosystems.clearth.data.DataHandlersFactory;
import com.exactprosystems.clearth.messages.PlainMessageSender;

import java.util.List;
import java.util.Set;

public interface ClearThMessageConnection extends ClearThRunnableConnection, PlainMessageSender
{
	void addListener(ListenerConfiguration listener);

	void removeListener(ListenerConfiguration listener);

	void removeAllListeners();

	List<ListenerConfiguration> getListeners();

	Set<Class<? extends MessageListener>> getSupportedListenerTypes();

	Class<?> getListenerClass(String type);
	
	MessageListener findListener(String listenerType);
	
	DataHandlersFactory getDataHandlersFactory();
	void setDataHandlersFactory(DataHandlersFactory dataHandlersFactory);
	
	long getSent();
	long getReceived();
}
