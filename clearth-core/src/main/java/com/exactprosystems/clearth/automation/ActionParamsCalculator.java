/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.async.WaitAsyncEnd;
import com.exactprosystems.clearth.utils.ObjectWrapper;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

public class ActionParamsCalculator
{
	private static final Logger logger = LoggerFactory.getLogger(ActionParamsCalculator.class);
	public static final String PARAMETER_ERROR_FORMAT = "'%s' - formula '%s' returned value '%s'";
	
	private final MatrixFunctions matrixFunctions;
	private Action action;
	private List<String> errors;
	private boolean warnOnError;
	
	public ActionParamsCalculator(MatrixFunctions matrixFunctions)
	{
		this.matrixFunctions = matrixFunctions;
	}
	
	
	protected Logger getLogger()
	{
		return logger;
	}
	
	protected MatrixFunctions getMatrixFunctions()
	{
		return matrixFunctions;
	}
	
	protected Action getAction()
	{
		return action;
	}

	public List<String> getErrors()
	{
		return errors;
	}

	public boolean isWarnOnError()
	{
		return warnOnError;
	}


	public List<String> calculateParameters(Action action, boolean warnOnError)
	{
		Matrix matrix = action.getMatrix();
		MvelVariables mvelVars = matrix.getMvelVars();
		mvelVars.saveInputParams(action);

		calculateMainParameters(action, new ArrayList<String>(), warnOnError);
		calculateSpecialParameters();
		
		Map<String, String> params = action.getInputParams();
		matrixFunctions.setCurrentTime(Calendar.getInstance());
		for (Entry<String, String> param : params.entrySet())
		{
			String value = calculateParameter(param.getValue(), param.getKey());
			if (value != null)
				params.put(param.getKey(), value);
		}
		matrixFunctions.setCurrentTime(null);
		
		return errors;
	}

	private void calculateSpecialParameters()
	{
		Map<String, String> formulas = action.getSpecialParamsFormulas();
		if (formulas == null)
			return;
		
		for (String param : action.getSpecialParamsNames())
		{
			String value = calculateParameter(formulas.get(param), param);
			if (value != null)
				action.getSpecialParams().put(param, value);
		}
	}
	
	public void calculateMainParameters(Action action, List<String> errorsOutput, boolean warnOnError)
	{
		this.action = action;
		this.errors = errorsOutput;
		this.warnOnError = warnOnError;
		
		calculateExecute();
		calculateComment();
		calculateTimeout(); 
		calculateInverted();
		calculateAsync();
		calculateAsyncGroup();
		calculateWaitAsyncEnd();
		calculateIdInTemplate();
	}
	
	public String buildParameterError(String name, String expression, String value)
	{
		return String.format(PARAMETER_ERROR_FORMAT, name, expression, value);
	}
	
	
	protected String calculateParameter(String valueExpression, String parameterName)
	{
		if (valueExpression == null)
			return null;

		MvelVariables mvelVars = action.getMatrix().getMvelVars();
		try
		{
			Object valueObj = matrixFunctions.calculateExpression(valueExpression, parameterName,
					mvelVars.getVariables(), mvelVars.getFixedIds(), getAction(), new ObjectWrapper(0));
			
			String value = (valueObj != null) ? valueObj.toString() : null;
			mvelVars.saveCalculatedParameter(action.getIdInMatrix(), parameterName, value);
			return value;
		}
		catch (Exception e)
		{
			if (warnOnError && action.isExecutable())
				getLogger().warn("Error while calculating parameter '{}'", parameterName, e);
			
			if (errors != null)
			{
				String error = "'"+parameterName+"' - "+MatrixFunctions.errorToText(e);
				errors.add(error);
			}
			return null;
		}
	}
	
	protected void calculateExecute()
	{
		String formula = action.getFormulaExecutable(),
				execute = calculateParameter(formula, ActionGenerator.COLUMN_EXECUTE);
		if (execute == null)
			return;
		
		if (InputParamsUtils.YES.contains(execute.toLowerCase()))
			action.setExecutable(true);
		else if (InputParamsUtils.NO.contains(execute.toLowerCase()))
		{
			action.setExecutable(false);
			action.getMatrix().getNonExecutableActions().add(action.getIdInMatrix());
		}
		else
		{
			action.setExecutable(true);
			if (errors != null)
				errors.add(buildParameterError(ActionGenerator.COLUMN_EXECUTE, formula, execute));
		}
	}
	
	protected void calculateComment()
	{
		String formula = action.getFormulaComment(),
				comment = calculateParameter(formula, ActionGenerator.COLUMN_COMMENT);
		if (comment == null)
		{
			if (formula != null)
				action.setComment(formula);
		}
		else
			action.setComment(comment);
	}
	
	protected void calculateTimeout()
	{
		String formula = action.getFormulaTimeout(),
				timeout = calculateParameter(formula, ActionGenerator.COLUMN_TIMEOUT);
		if (timeout == null)
		{
			if (formula != null)
				action.setTimeOut(0);
			return;
		}
		
		try
		{
			action.setTimeOut(Integer.parseInt(timeout));
		}
		catch (Exception e)
		{
			action.setTimeOut(0);
			if (errors != null)
				errors.add(buildParameterError(ActionGenerator.COLUMN_TIMEOUT, formula, timeout));
		}
	}
	
	protected void calculateInverted()
	{
		String formula = action.getFormulaInverted(),
				inverted = calculateParameter(formula, ActionGenerator.COLUMN_INVERT);
		if (inverted == null)
			return;
		
		if (InputParamsUtils.YES.contains(inverted.toLowerCase()))
			action.setInverted(true);
		else if (InputParamsUtils.NO.contains(inverted.toLowerCase()))
			action.setInverted(false);
		else
		{
			if (errors != null)
				errors.add(buildParameterError(ActionGenerator.COLUMN_INVERT, formula, inverted));
		}
	}
	
	protected void calculateAsync()
	{
		String formula = action.getFormulaAsync(),
				async = calculateParameter(formula, ActionGenerator.COLUMN_ASYNC);
		if (async == null)
			return;
		
		if (InputParamsUtils.YES.contains(async.toLowerCase()))
			action.setAsync(true);
		else if (InputParamsUtils.NO.contains(async.toLowerCase()))
			action.setAsync(false);
		else
		{
			if (errors != null)
				errors.add(buildParameterError(ActionGenerator.COLUMN_ASYNC, formula, async));
		}
	}
	
	protected void calculateAsyncGroup()
	{
		String formula = action.getFormulaAsyncGroup(),
				group = calculateParameter(formula, ActionGenerator.COLUMN_ASYNCGROUP);
		if (group != null)
			action.setAsyncGroup(group);
	}
	
	protected void calculateWaitAsyncEnd()
	{
		String formula = action.getFormulaWaitAsyncEnd(),
				wait = calculateParameter(formula, ActionGenerator.COLUMN_WAITASYNCEND);
		if (wait != null)
			action.setWaitAsyncEnd(WaitAsyncEnd.byLabel(wait));
	}
	
	protected void calculateIdInTemplate()
	{
		String formula = action.getFormulaIdInTemplate();
		String idInMatrixGenerator = calculateParameter(formula, ActionGenerator.COLUMN_ID_IN_TEMPLATE);
		if (idInMatrixGenerator != null)
			action.setIdInTemplate(idInMatrixGenerator);
	}
}
