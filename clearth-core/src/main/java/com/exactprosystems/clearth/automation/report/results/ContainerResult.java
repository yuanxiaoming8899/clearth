/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.report.results;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result which can include other results.
 * @author daria.plotnikova
 */
public class ContainerResult extends Result implements Serializable
{
	private static final long serialVersionUID = -2015931472252216998L;
	
	protected ContainerResult parentResult = null;
	protected final List<Result> details;
	protected String header;
	protected boolean blockView = false, hasStatus = true, useFailReasonColor = false;
	
	public ContainerResult()
	{
		super();
		this.details = new ArrayList<>();
	}
	
	protected ContainerResult(String header, boolean isBlockView)
	{
		this.header = header;
		this.blockView = isBlockView;
		this.details = new ArrayList<>();
	}
	
	protected ContainerResult(String header, boolean isBlockView, List<Result> details)
	{
		this(header, isBlockView);
		
		// Add details one by one to check their successes and fail reasons
		if (details != null)
		{
			for (Result detail : details)
				addDetail(detail);
		}
	}
	
	/**
	 * Creates a blank result with a header which may contain other results.
	 */
	public static ContainerResult createPlainResult(String header)
	{
		return new ContainerResult(header, false);
	}
	
	/**
	 * Creates a result with a header and specified details.
	 */
	public static ContainerResult createPlainResult(String header, List<Result> details)
	{
		return new ContainerResult(header, false, details);
	}
	
	/**
	 * Creates an open/close block which could contain results inside.
	 */
	public static ContainerResult createBlockResult(String header)
	{
		return new ContainerResult(header, true);
	}
	
	/**
	 * Creates an open/close block with specified results in it.
	 */
	public static ContainerResult createBlockResult(String header, List<Result> details)
	{
		return new ContainerResult(header, true, details);
	}
	
	
	/**
	 * Adds new result into the container.
	 */
	public void addDetail(Result detail)
	{
		details.add(detail);
		initDetail(detail);
	}

	protected void initDetail(Result detail)
	{
		if (detail instanceof ContainerResult)
			((ContainerResult) detail).parentResult = this;
	}

	@Override
	public boolean isSuccessWithoutInversionRegard()
	{
		return checkDetails();
	}

	protected boolean checkDetails()
	{
		if (!success)
			return false;
		for (Result detail : details)
		{
			if (!detail.isSuccess())
			{
				setSuccess(false);
				setFailReason(detail.getFailReason());
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void processDetails(File reportDir, Action linkedAction)
	{
		details.forEach(detail -> detail.processDetails(reportDir, linkedAction));
	}
	
	@Override
	public void clearDetails()
	{
		details.clear();
	}
	
	@Override
	public void setSuccess(boolean success)
	{
		super.setSuccess(success);
		if (parentResult != null && !success)
			parentResult.setSuccess(false);
	}
	
	@Override
	public void setFailReason(FailReason failReason)
	{
		this.failReason = failReason;
		if (parentResult != null)
			parentResult.setFailReason(failReason);
	}
	
	@Override
	public FailReason getFailReason()
	{
		checkDetails();
		return failReason;
	}
	
	@Override
	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		checkDetails();
		super.toLineBuilder(builder, prefix);
		builder.add(prefix).add("Details (Identical / Exp. / Act.)").eol();
		for (Result detail : details)
			detail.toLineBuilder(builder, prefix + " ");
		return builder;
	}
	
	
	public List<Result> getDetails()
	{
		return details;
	}
	
	public void setHeader(String header)
	{
		this.header = header;
	}
	
	public String getHeader()
	{
		return header;
	}
	
	public void setBlockView(boolean blockView)
	{
		this.blockView = blockView;
	}
	
	public boolean isBlockView()
	{
		return blockView;
	}
	
	/**
	 * If it's {@code false}, container's header becomes black and its status doesn't counted.
	 * Default value is {@code true}.
	 * Applicable only for block-viewed containers.
	 */
	public void setHasStatus(boolean hasStatus)
	{
		this.hasStatus = hasStatus;
	}
	
	public boolean isHasStatus()
	{
		return hasStatus;
	}
	
	/**
	 * By default if container's result is fault its color is red.
	 * Provide {@code true} here if you need to use fail reason color (for example, yellow one for {@code FailReason.COMPARISON}).
	 * Applicable only for block-viewed containers.
	 */
	public void setUseFailReasonColor(boolean useFailReasonColor)
	{
		this.useFailReasonColor = useFailReasonColor;
	}
	
	public boolean isUseFailReasonColor()
	{
		return useFailReasonColor;
	}
}
