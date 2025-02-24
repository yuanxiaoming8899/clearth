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

package com.exactprosystems.clearth.data.th2;

import java.util.Collection;

import com.exactpro.th2.common.grpc.Event;
import com.exactpro.th2.common.grpc.EventBatch;
import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.StepMetadata;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.data.TestExecutionHandlingException;
import com.exactprosystems.clearth.data.th2.events.EventFactory;
import com.exactprosystems.clearth.data.th2.events.EventUtils;
import com.exactprosystems.clearth.data.th2.events.ResultSaver;
import com.exactprosystems.clearth.data.th2.events.Th2EventId;
import com.exactprosystems.clearth.data.th2.events.Th2EventMetadata;
import com.exactprosystems.clearth.data.HandledTestExecutionId;
import com.exactprosystems.clearth.data.TestExecutionHandler;

public class Th2TestExecutionHandler implements TestExecutionHandler
{
	private final MessageRouter<EventBatch> router;
	private final SchedulerExecutionInfo executionInfo;
	private final EventFactory eventFactory;
	private final ResultSaver resultSaver;
	
	public Th2TestExecutionHandler(String schedulerName, MessageRouter<EventBatch> router, EventFactory eventFactory, ResultSaver resultSaver)
	{
		this.router = router;
		this.executionInfo = new SchedulerExecutionInfo(schedulerName);
		this.eventFactory = eventFactory;
		this.resultSaver = resultSaver;
	}
	
	
	@Override
	public void close() throws Exception
	{
		router.close();
	}
	
	
	@Override
	public void onTestStart(Collection<String> matrices, GlobalContext globalContext) throws TestExecutionHandlingException
	{
		executionInfo.setFromGlobalContext(globalContext);
		
		EventID id = storeSchedulerInfo();
		executionInfo.setEventId(id);
		
		storeMatricesInfo(matrices);
	}
	
	@Override
	public void onTestEnd() throws TestExecutionHandlingException
	{
	}
	
	@Override
	public void onGlobalStepStart(StepMetadata metadata) throws TestExecutionHandlingException
	{
		for (MatrixExecutionInfo mi : executionInfo.getMatrixInfos())
			startStepInMatrix(metadata, mi);
	}
	
	@Override
	public void onGlobalStepEnd()
	{
	}
	
	@Override
	public HandledTestExecutionId onAction(Action action) throws TestExecutionHandlingException
	{
		if (action.isSubaction())
			return null;
		
		Event actionEvent = eventFactory.createActionEvent(action, executionInfo);
		storeEvent(actionEvent);
		
		EventBatch subEvents = eventFactory.createActionSubEvents(action, actionEvent);
		storeBatch(subEvents);
		
		return new Th2EventId(actionEvent.getId());
	}
	
	@Override
	public void onActionResult(Result result, Action action) throws TestExecutionHandlingException
	{
		if (action.isSubaction())
			return;
		
		EventID eventId = getEventId(action);
		Th2EventMetadata metadata = new Th2EventMetadata(eventId, 
				EventUtils.getActionStartTimestamp(action), 
				EventUtils.getActionEndTimestamp(action));
		
		Event outputParams = eventFactory.createOutputParamsEvent(action, metadata);
		if (outputParams != null)
			storeEvent(outputParams);
		
		Event status = eventFactory.createActionStatusEvent(action, result, metadata);
		storeEvent(status);
		
		if (result != null && !(result instanceof DefaultResult))  //Details of DefaultResult are saved in action status event
			resultSaver.storeResult(result, metadata);
	}
	
	@Override
	public boolean isActive()
	{
		return true;
	}
	
	
	protected EventID storeSchedulerInfo() throws TestExecutionHandlingException
	{
		Event event = eventFactory.createSchedulerEvent(executionInfo);
		EventID result = event.getId();
		storeEvent(event);
		return result;
	}
	
	protected void storeMatricesInfo(Collection<String> matrices) throws TestExecutionHandlingException
	{
		EventID parentId = executionInfo.getEventId();
		for (String m : matrices)
		{
			Event event = eventFactory.createMatrixEvent(m, executionInfo.getStartTimestamp(), parentId);
			EventID id = event.getId();
			MatrixExecutionInfo info = new MatrixExecutionInfo(m, id);
			executionInfo.setMatrixInfo(m, info);
			storeEvent(event);
		}
	}
	
	protected void startStepInMatrix(StepMetadata metadata, MatrixExecutionInfo matrixInfo) throws TestExecutionHandlingException
	{
		Event event = eventFactory.createStepEvent(metadata, matrixInfo.getEventId());
		EventID id = event.getId();
		matrixInfo.setStepEventId(metadata.getName(), id);
		storeEvent(event);
	}
	
	
	private void storeEvent(Event event) throws TestExecutionHandlingException
	{
		EventBatch batch = EventUtils.wrap(event);
		storeBatch(batch);
	}
	
	private void storeBatch(EventBatch batch) throws TestExecutionHandlingException
	{
		try
		{
			router.send(batch);
		}
		catch (Exception e)
		{
			throw new TestExecutionHandlingException(e);
		}
	}
	
	private EventID getEventId(Action action) throws TestExecutionHandlingException
	{
		HandledTestExecutionId id = action.getTestExecutionId();
		if (id == null)
			throw new TestExecutionHandlingException("Action doesn't contain handled test execution ID");
		if (!(id instanceof Th2EventId))
			throw new TestExecutionHandlingException("Handled test execution ID must be of class "+Th2EventId.class.getCanonicalName()+", but it is "+id.getClass().getCanonicalName());
		return ((Th2EventId)id).getId();
	}
}
