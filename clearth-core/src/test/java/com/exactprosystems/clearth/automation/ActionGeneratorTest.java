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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.config.MatrixFatalErrors;
import com.exactprosystems.clearth.config.SpecialActionParameters;
import com.exactprosystems.clearth.utils.SettingsException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;

public class ActionGeneratorTest
{
	public static final String matrixDir = "ActionGeneratorMatrixIDTest";
	public static Path matrixPath;

	@DataProvider(name = "testParams")
	Object[][] getParams()
	{
		return new Object[][]
			{
				{
					new MatrixData(), "Matrix", matrixPath.resolve("matrixDuplicatedID.csv").toFile(),
					new ArrayList<Matrix>(), true, false
				},
				{
					new MatrixData(), "Matrix", matrixPath.resolve("matrixDuplicatedID.csv").toFile(),
					new ArrayList<Matrix>(), false, false
				}
			};
	}

	@BeforeClass
	public static void init() throws FileNotFoundException
	{
		matrixPath = Paths.get(resourceToAbsoluteFilePath(matrixDir));
	}

	@Test(dataProvider = "testParams")
	public void checkMatrixFatalErrors(MatrixData matrixData, String matrixName, File file, List<Matrix> matrices,
	                                   boolean idError, boolean onlyCheck)
			throws IOException, SettingsException
	{
		matrixData.setName(matrixName);
		matrixData.setFile(file);
		ActionGenerator generator = createGenerator(matrices, idError);
		generator.build(matrixData, onlyCheck);
		Assert.assertEquals(matrices.get(0).isHasFatalErrors(), idError);
	}

	protected ActionGenerator createGenerator(List<Matrix> matrices, boolean idError) throws SettingsException
	{
		Map<String, Step> steps = new HashMap<>();
		steps.put("One", new DefaultStep("One", null, null, StartAtType.DEFAULT, false,
						null, false, false, true, null));
		steps.put("Two", new DefaultStep("Two", null, null, StartAtType.DEFAULT, false,
						null, false, false, true, null));

		ActionFactory actionFactory = new ActionFactory();
		actionFactory.loadActionsMapping(matrixPath.resolve("actionsmapping.cfg"));

		MatrixFunctions matrixFunctions = new MatrixFunctions(null, null, null, false, null);

		MatrixFatalErrors matrixFatalErrors = new MatrixFatalErrors();
		matrixFatalErrors.setDuplicateActionId(idError);

		SpecialActionParameters specialActionParameters = new SpecialActionParameters();
		Set<String> params = new HashSet<>();
		params.add("Params");
		specialActionParameters.setParameters(params);

		ActionGeneratorResources resources = new ActionGeneratorResources(specialActionParameters, actionFactory,
				new MvelVariablesFactory(), matrixFunctions, matrixFatalErrors);

		return new DefaultActionGenerator(steps, matrices, new HashMap<String, Preparable>(), resources);
	}

	@Test
	public void specialParameters() throws SettingsException, IOException
	{
		Path parentDir = Paths.get("src", "test", "resources", "ActionGenerator");
		String testCaseParam = "TestCase",
				groupParam = "Group",
				testCaseValue = "Dummy check",
				groupValue = "Group 1",
				stepName = "Step1";

		Map<String, Step> steps = new HashMap<>();
		steps.put(stepName, new DefaultStep(stepName, null, null, StartAtType.DEFAULT, false, null, false, false, true, null));

		List<Matrix> matrices = new ArrayList<>();
		Map<String, Preparable> preparable = new HashMap<>();

		SpecialActionParameters specialActionParameters = new SpecialActionParameters();
		Set<String> params = new HashSet<>();
		params.add(testCaseParam);
		params.add(groupParam);
		specialActionParameters.setParameters(params);

		ActionFactory actionFactory = new ActionFactory();
		actionFactory.loadActionsMapping(parentDir.resolve("actionsmapping.cfg"));
		MvelVariablesFactory mvelFactory = new MvelVariablesFactory();
		MatrixFunctions mf = new MatrixFunctions(null, null, null, false, null);
		ActionGeneratorResources resources = new ActionGeneratorResources(specialActionParameters, actionFactory, mvelFactory,
				mf, new MatrixFatalErrors());

		MatrixData matrixData = new MatrixData();
		matrixData.setName("Matrix1");
		matrixData.setFile(parentDir.resolve("special_params.csv").toFile());

		ActionGenerator generator = new DefaultActionGenerator(steps, matrices, preparable, resources);
		Assert.assertTrue(generator.build(matrixData, false), "Action generation result");

		List<Action> actions = matrices.get(0).getActions();

		Map<String, String> noSpecial = actions.get(0).getSpecialParams();
		Assert.assertNull(noSpecial, "Special parameters in action when they are absent in matrix");

		Action actionWithIncompleteSpecial = actions.get(1);
		Map<String, String> incompleteSpecial = actionWithIncompleteSpecial.getSpecialParams();
		SoftAssert incompleteSoft = new SoftAssert();
		incompleteSoft.assertEquals(incompleteSpecial.size(), 1, "Number of special parameters when incomplete set is used");
		incompleteSoft.assertEquals(incompleteSpecial.get(testCaseParam), testCaseValue, "Value of special parameter");
		incompleteSoft.assertNull(actionWithIncompleteSpecial.getInputParam(testCaseParam), "Special parameter among regular parameters");
		incompleteSoft.assertAll();

		Action actionWithLastSpecial = actions.get(2);
		Map<String, String> lastSpecial = actionWithLastSpecial.getSpecialParams();
		SoftAssert lastSoft = new SoftAssert();
		lastSoft.assertEquals(lastSpecial.size(), 1, "Number of special parameters when only last one is used");
		lastSoft.assertEquals(lastSpecial.get(groupParam), groupValue, "Value of special parameter");
		lastSoft.assertNull(actionWithLastSpecial.getInputParam(groupParam), "Special parameter among regular parameters");
		lastSoft.assertAll();

		Action actionWithAllSpecial = actions.get(3);
		Map<String, String> allSpecial = actionWithAllSpecial.getSpecialParams();
		SoftAssert allSoft = new SoftAssert();
		allSoft.assertEquals(allSpecial.size(), 2, "Number of special parameters");
		allSoft.assertEquals(allSpecial.get(testCaseParam), testCaseValue, "Value of "+testCaseParam);
		allSoft.assertEquals(allSpecial.get(groupParam), groupValue, "Value of "+groupParam);
		allSoft.assertNull(actionWithAllSpecial.getInputParam(testCaseParam), "Special parameter "+testCaseParam+" among regular parameters");
		allSoft.assertNull(actionWithAllSpecial.getInputParam(groupParam), "Special parameter "+groupParam+" among regular parameters");
		allSoft.assertAll();
	}
}