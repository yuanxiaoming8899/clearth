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

package com.exactprosystems.clearth;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.report.html.template.ReportTemplatesProcessor;
import com.exactprosystems.clearth.automation.schedulerinfo.SchedulerInfoExporter;
import com.exactprosystems.clearth.automation.schedulerinfo.template.SchedulerInfoTemplatesProcessor;
import com.exactprosystems.clearth.config.Automation;
import com.exactprosystems.clearth.config.ClearThConfiguration;
import com.exactprosystems.clearth.config.ConnectionType;
import com.exactprosystems.clearth.config.MemoryMonitorCfg;
import com.exactprosystems.clearth.connectivity.CodecsStorage;
import com.exactprosystems.clearth.connectivity.ConnectionsTransmitter;
import com.exactprosystems.clearth.connectivity.FavoriteConnectionManager;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.connections.storage.DefaultClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.iface.DefaultCodecFactory;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.ICodecFactory;
import com.exactprosystems.clearth.connectivity.validation.ClearThConnectionValidationRule;
import com.exactprosystems.clearth.data.DataHandlersFactory;
import com.exactprosystems.clearth.generators.IncrementingValueGenerators;
import com.exactprosystems.clearth.tools.ToolsFactory;
import com.exactprosystems.clearth.tools.ToolsManager;
import com.exactprosystems.clearth.utils.*;
import com.exactprosystems.clearth.utils.sql.ClearThDbConnection;
import com.exactprosystems.clearth.xmldata.*;
import com.exactprosystems.memorymonitor.MemoryMonitor;
import freemarker.template.TemplateModelException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public abstract class ClearThCore
{
	protected static ClearThCore instance = null;

	private ConfigFiles configFiles;
	private MemoryMonitor memoryMonitor;
	
	protected String root, approot, logsDir;
	protected boolean logsOverride, rootOverride;
	protected UsersManager usersManager;
	protected ValueGenerators valueGenerators;
	protected SchedulerFactory schedulerFactory;
	protected SchedulersManager schedulersManager;
	protected ActionFactory actionFactory;
	protected MatrixDataFactory matrixDataFactory;
	protected ICodecFactory codecFactory;
	protected FavoriteConnectionManager favoriteConnections;
	protected CodecsStorage codecs;
	protected ToolsManager toolsManager;
	protected MatrixFunctionsFactory<MatrixFunctions> matrixFunctionsFactory;
	protected MvelVariablesFactory mvelVariablesFactory;
	protected Map<String, XmlMessageConverterConfig> messageConverterConfigs;
	protected Map<String, XmlScriptConverterConfig> scriptConverterConfigs;
	protected ReportTemplatesProcessor reportTemplatesProcessor;
	protected SchedulerInfoTemplatesProcessor schedulerInfoTemplatesProcessor;
	protected Map<String, XmlMessageHelperConfig> messageHelpers;
	protected Map<String, String> replacedLocations;
	protected ClearThConnectionStorage connectionStorage;
	
	protected SchedulerSettingsTransmitter schedulerSettingsTransmitter;
	protected SchedulerInfoExporter schedulerInfoExporter;
	protected ConnectionsTransmitter connectionsTransmitter;
	
	protected ClearThVersion cthVersion;

	protected ComparisonUtils comparisonUtils;

	protected ClearThConfiguration config;
	
	protected DataHandlersFactory dataHandlersFactory;

	public static <T extends ClearThCore> T getInstance()
	{
		return (T)instance;
	}
	
	
	public static String filesRoot()
	{
		return instance.getFilesRoot();
	}

	public static String rootRelative(String fileName)
	{
		return instance.getRootRelative(fileName);
	}

	public static String appRoot()
	{
		return instance.getAppRoot();
	}
	
	public static String appRootRelative(String fileName)
	{
		return instance.getAppRootRelative(fileName);
	}

	public static boolean rootOverridden()
	{
		return instance.isRootOverridden();
	}
	
	public static String schedulersPath()
	{
		return instance.getSchedulersPath();
	}
	
	public static String userSettingsPath()
	{
		return instance.getUserSettingsPath();
	}
	
	public static String usersListPath()
	{
		return instance.getUsersListPath();
	}
	
	public static String dictsPath()
	{
		return instance.getDictsPath();
	}
	
	public static String connectionsPath()
	{
		return instance.getConnectionsPath();
	}

	public static String fileConnectionsPath()
	{
		return instance.getFileConnectionsPath();
	}

	public static String dbConnectionsPath()
	{
		return instance.getDbConnectionsPath();
	}

	public static String htmlTemplatesPath()
	{
		return instance.getHtmlTemplatesPath();
	}

	public static String templatesPath()
	{
		return instance.getTemplatesPath();
	}
	
	public static String automationStoragePath()
	{
		return instance.getAutomationStoragePath();
	}
	
	public static String scriptsPath()
	{
		return instance.getScriptsPath();
	}
	
	public static String reportsPath()
	{
		return instance.getReportsPath();
	}

	public static String lastExecutionPath()
	{
		return instance.getLastExecutionPath();
	}

	public static String uploadStoragePath()
	{
		return instance.getUploadStoragePath();
	}

	public static String tempPath()
	{
		return instance.getTempDirPath();
	}

	public static String dataPath()
	{
		return instance.getDataDirPath();
	}

	public static ConfigFiles configFiles()
	{
		return instance.getConfigFiles();
	}
	
	public static ValueGenerator commonGenerator()
	{
		return instance.getCommonGenerator();
	}
	
	public static ValueGenerators valueGenerators()
	{
		return instance.getValueGenerators();
	}
	
	public static ClearThConnectionStorage connectionStorage()
	{
		return instance.getConnectionStorage();
	}

	public static ClearThConfiguration config()
	{
		return instance.getConfig();
	}
	
	public ClearThCore() throws ClearThException
	{
		if (ClearThCore.instance != null)
			throw new ClearThException("Application already instantiated");
		
		ClearThCore.instance = this;
	}
	
	public void init(ConfigFiles configFiles, DeploymentConfig depConfig, Object... otherEntities) throws ClearThException
	{
		if (root != null)
			throw new ClearThException("Application already initialized");
		
		this.configFiles = configFiles;
		initSystemProperties();
		initPaths(depConfig);
		initLogging();
		
		Thread.setDefaultUncaughtExceptionHandler(createUncaughtExceptionHandler());
		
		//Configuring application according to configuration files
		try
		{
			config = loadConfig(depConfig.getConfigFileName());
			getLogger().info("Using configuration: {}", config);
			
			replacedLocations = config.getLocationsMapping();
			
			createDefaultDirs();
			createDirs();
			reconfigureLogging();
			dataHandlersFactory = createDataHandlersFactory();
			valueGenerators = createValueGenerators();
			connectionStorage = createConnectionStorage();
			actionFactory = createActionFactory();
			matrixDataFactory = createMatrixDataFactory();
			codecFactory = createCodecFactory();
			schedulerSettingsTransmitter = createSchedulerSettingsTransmitter();
			schedulerInfoExporter = createSchedulerInfoExporter();
			connectionsTransmitter = createConnectionsTransmitter();
			matrixFunctionsFactory = createMatrixFunctionsHolder();
			mvelVariablesFactory = createMvelVariablesFactory();
			schedulerFactory = createSchedulerFactory(valueGenerators);
			memoryMonitor = createMemoryMonitor();
			if (memoryMonitor != null)
				memoryMonitor.start();
			
			usersManager = createUsersManager();
			usersManager.init();
			codecs = loadCodecs(getRootRelative(configFiles.getCodecsFileName()));
			schedulersManager = createSchedulersManager(getSchedulersPath(), getRootRelative(configFiles.getSchedulersFileName()));
			
			messageConverterConfigs = loadMessageConverters(getRootRelative(configFiles.getMessageConvertersFileName()));
			scriptConverterConfigs = loadScriptConverters(getRootRelative(configFiles.getScriptConvertersFileName()));
			messageHelpers = loadMessageHelpers(getRootRelative(configFiles.getMessageHelpersFileName()));
			favoriteConnections = createFavoriteConnectionManager(getUserSettingsPath());
			toolsManager = createToolsManager();
			comparisonUtils = createComparisonUtils();
			actionFactory.loadActionsMapping(configFiles);
			
			reportTemplatesProcessor = createReportTemplatesProcessor();
			configureReportTemplates(reportTemplatesProcessor);
			schedulerInfoTemplatesProcessor = createSchedulerInfoTemplatesProcessor();
			prepareRealTimeReport();
			
			initSchedulersManager();
			initConnectionStorage();
			initFavoriteConnectionManager();
			initOtherEntities(otherEntities);  //Point of extension with project-specific objects
			initFinish(depConfig);
		}
		catch (Throwable e)
		{
			String msg = "FATAL ERROR occurred on initialization";
			getLogger().error(msg, e);
			throw new ClearThException(msg, e);
		}
	}
	
	
	/*** init() parts ***/
	
	public void configureLogging()
	{
		File logConfig = new File(getRootRelative(configFiles.getLogCfgFileName()));
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		ctx.setConfigLocation(logConfig.toURI());
		
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	//Logging is initialized before config
	//Reconfigure logger to possibly use replacement path from config
	protected void reconfigureLogging()
	{
		configureLogging();
	}
	
	protected void initSystemProperties()
	{
		Locale.setDefault(Locale.ENGLISH);
		System.setOut(new SysoutFilter(System.out));
		
		System.setProperty("mvel2.disable.jit", "true");
	}
	
	protected void initPaths(DeploymentConfig depConfig)
	{
		logsDir = depConfig.getLogsDir();
		logsOverride = depConfig.isLogsDirOverride();
		approot = depConfig.getAppRoot();
		root = depConfig.getRoot();
		rootOverride = depConfig.isRootOverride();
	}
	
	protected void initLogging()
	{
		configureLogging();
	}
	
	
	protected ClearThConnectionStorage createConnectionStorage() throws ClearThException
	{
		return new DefaultClearThConnectionStorage(dataHandlersFactory);
	}
	
	protected void initConnectionStorage() throws ClearThException
	{
		registerJdbcDrivers();
		registerConnectionTypes();
		connectionStorage.loadConnections();
		connectionStorage.autoStartConnections();
	}
	
	protected ActionFactory createActionFactory()
	{
		return new ActionFactory();
	}
	
	protected MatrixDataFactory createMatrixDataFactory()
	{
		return new DefaultMatrixDataFactory();
	}
	
	protected MvelVariablesFactory createMvelVariablesFactory()
	{
		return new MvelVariablesFactory();
	}
	
	protected ICodecFactory createCodecFactory()
	{
		return new DefaultCodecFactory();
	}
	
	protected SchedulerSettingsTransmitter createSchedulerSettingsTransmitter()
	{
		return new SchedulerSettingsTransmitter();
	}

	protected SchedulerInfoExporter createSchedulerInfoExporter()
	{
		return new SchedulerInfoExporter();
	}

	protected ConnectionsTransmitter createConnectionsTransmitter()
	{
		return new ConnectionsTransmitter(new File(tempPath()), connectionStorage);
	}

	protected MatrixFunctionsFactory createMatrixFunctionsHolder()
	{
		return new DefaultMatrixFunctionsFactory();
	}
	
	protected void createDefaultDirs() throws IOException
	{
		Files.createDirectories(Paths.get(getConnectionsPath()));
		Files.createDirectories(Paths.get(getUploadStoragePath()));
		Files.createDirectories(Paths.get(getTempDirPath()));
		Files.createDirectories(Paths.get(getDataDirPath()));
		Files.createDirectories(Paths.get(getSchedulersPath()));
		Files.createDirectories(Paths.get(getUserSettingsPath()));
		Files.createDirectories(Paths.get(getDictsPath()));
		Files.createDirectories(Paths.get(getAutomationStoragePath()));
		Files.createDirectories(Paths.get(getScriptsPath()));
		Files.createDirectories(Paths.get(getLastExecutionPath()));
	}
	
	protected DataHandlersFactory createDataHandlersFactory() throws ClearThException
	{
		String className = config.getData().getDataHandlersFactory();
		try
		{
			return (DataHandlersFactory) Class.forName(className).getConstructor().newInstance();
		}
		catch (Exception e)
		{
			throw new ClearThException("Could not create data handlers factory by class "+className, e);
		}
	}
	
	protected ValueGenerators createValueGenerators()
	{
		return new IncrementingValueGenerators();
	}
	
	protected ExecutorFactory createExecutorFactory(ValueGenerators valueGenerators)
	{
		return new DefaultExecutorFactory(valueGenerators.getCommonGenerator());
	}
	
	protected StepFactory createStepFactory()
	{
		return new DefaultStepFactory();
	}
	
	protected ActionGeneratorResources createActionGeneratorResources(ValueGenerators valueGenerators)
	{
		Automation automation = config.getAutomation();
		return new ActionGeneratorResources(automation.getSpecialActionParameters(), actionFactory, mvelVariablesFactory,
				createMatrixFunctions(Collections.emptyMap(), null, null, true, valueGenerators.getCommonGenerator()),
				automation.getMatrixFatalErrors());
	}
	
	protected SchedulerFactory createSchedulerFactory(ValueGenerators valueGenerators)
	{
		return new DefaultSchedulerFactory(createExecutorFactory(valueGenerators), createStepFactory(), createActionGeneratorResources(valueGenerators));
	}
	
	protected MemoryMonitor createMemoryMonitor()
	{
		MemoryMonitorCfg mmCfg = config().getMemory().getMonitor();
		return new MemoryMonitor("MemoryMonitor", mmCfg.getSleep(), mmCfg.getLargeDiff(), mmCfg.getLowMemory());
	}
	
	protected UsersManager createUsersManager()
	{
		return new UsersManager();
	}

	protected void registerJdbcDrivers() throws ClearThException
	{
		List<String> driverClasses = config.getConnectivity().getJdbcDrivers().getDriverClassNames();
		for(String driverClass: driverClasses)
		{
			try
			{
				Driver driver = (Driver) Class.forName(driverClass).newInstance();
				DriverManager.registerDriver(driver);
			} catch (Exception e)
			{
				throw new ClearThException("JDBC driver '" + driverClass + "' not registered: " + e.getMessage(), e);
			}
		}
	}

	protected void registerConnectionTypes() throws ClearThException
	{
		for (ConnectionType type : config.getConnectivity().getTypesConfig().getTypes())
		{
			try
			{
				Set<ClearThConnectionValidationRule> validationRules = type.getValidationRules().createRules();
				
				ConnectionTypeInfo typeInfo = new ConnectionTypeInfo(type.getName(), 
						Class.forName(type.getConnectionClass()).asSubclass(ClearThConnection.class), 
						Paths.get(rootRelative(type.getDirectory())), 
						validationRules);
				connectionStorage.registerType(typeInfo);
			}
			catch (Exception e)
			{
				throw new ClearThException("Could not register connection type '"+type.getName()+"'", e);
			}
		}
	}
	
	protected CodecsStorage loadCodecs(String codescFileName) throws Exception
	{
		getLogger().debug("Loading codecs");

		File file = new File(codescFileName);
		if (!file.isFile())
			return new CodecsStorage(new ArrayList<XmlCodecConfig>());

		XmlCodecConfigs configs = XmlUtils.unmarshalObject(XmlCodecConfigs.class, codescFileName);
		return new CodecsStorage(configs.getCodecConfigs());
	}
	
	protected Map<String, XmlMessageConverterConfig> loadMessageConverters(String messageConvertersFileName) throws Exception
	{
		getLogger().debug("Loading message converters");
		Map<String, XmlMessageConverterConfig> result = new LinkedHashMap<String, XmlMessageConverterConfig>();
		File file = new File(messageConvertersFileName);
		if (!file.isFile())
			return result;
		
		XmlMessageConverterConfigs converters = XmlUtils.unmarshalObject(XmlMessageConverterConfigs.class, messageConvertersFileName);
		for(XmlMessageConverterConfig converter : converters.getMessageConverterConfigs())
			result.put(converter.getName(), converter);
		return result;
	}
	
	protected Map<String, XmlScriptConverterConfig> loadScriptConverters(String scriptConvertersFileName) throws Exception
	{
		getLogger().debug("Loading script converters");
		Map<String, XmlScriptConverterConfig> result = new LinkedHashMap<String, XmlScriptConverterConfig>();
		File file = new File(scriptConvertersFileName);
		if (!file.isFile())
			return result;
		
		XmlScriptConverterConfigs converters = XmlUtils.unmarshalObject(XmlScriptConverterConfigs.class, scriptConvertersFileName);
		for(XmlScriptConverterConfig converter : converters.getScriptConverterConfigs())
			result.put(converter.getName(), converter);
		return result;
	}

	protected Map<String, XmlMessageHelperConfig> loadMessageHelpers(String messageHelpersFileName) throws Exception
	{
		getLogger().debug("Loading message helpers");
		Map<String, XmlMessageHelperConfig> result = new LinkedHashMap<String, XmlMessageHelperConfig>();
		File file = new File(messageHelpersFileName);
		if (!file.isFile())
			return result;
		
		XmlMessageHelperConfigs converters = XmlUtils.unmarshalObject(XmlMessageHelperConfigs.class, messageHelpersFileName);
		for (XmlMessageHelperConfig helper : converters.getMessageHelperConfig())
			result.put(helper.getName(), helper);
		return result;
	}
	
	
	protected FavoriteConnectionManager createFavoriteConnectionManager(String userSettingsDir)
	{
		return new FavoriteConnectionManager(new File(userSettingsDir));
	}
	
	protected void initFavoriteConnectionManager()
	{
		favoriteConnections.loadFavoriteConnections();
	}
	
	
	protected ToolsManager createToolsManager()
	{
		return new ToolsManager(new File(getUserSettingsPath()));
	}
	
	protected ComparisonUtils createComparisonUtils()
	{
		return new ComparisonUtils();
	}
	
	protected ReportTemplatesProcessor createReportTemplatesProcessor() throws TemplateModelException, IOException
	{
		return new ReportTemplatesProcessor();
	}
	
	/**
	 * Configuration of report templates.
	 * Override this method to add support of custom Results.
	 *
	 * @param reportTemplatesProcessor
	 */
	protected void configureReportTemplates(@SuppressWarnings("unused") ReportTemplatesProcessor reportTemplatesProcessor)
	{
		/* Nothing to configure by default */
	}
	
	protected SchedulerInfoTemplatesProcessor createSchedulerInfoTemplatesProcessor() throws TemplateModelException, IOException
	{
		return new SchedulerInfoTemplatesProcessor();
	}
	
	private void prepareRealTimeReport()
	{
		try
		{
			String reportFilesPath = getReportsFilePath();
			String realTimePath = getRealTimeReportPath();
			File reportFilesDir = new File(reportFilesPath);
			File[] reportFiles = reportFilesDir.listFiles();
			if (reportFiles != null)
				for (File file : reportFiles)
					FileOperationUtils.copyFile(file.getName(), reportFilesPath, realTimePath);
		}
		catch (Exception e)
		{
			getLogger().warn("Error occurred while copying files for realtime report. It may work incorrectly", e);
		}
	}
	
	protected void initFinish(DeploymentConfig depConfig)
	{
		cthVersion = depConfig.getCTHVersion();
		getLogger().info("Version: {}", getVersion());
	}

	protected ClearThConfiguration loadConfig(String configFileName) throws Exception
	{
		return ClearThConfiguration.create(new File(configFileName));
	}

	public FavoriteConnectionManager getFavoriteConnections()
	{
		return favoriteConnections;
	}

	protected Thread.UncaughtExceptionHandler createUncaughtExceptionHandler()
	{
		return new DefaultExceptionHandler();
	}


	public ClearThConfiguration getConfig()
	{
		return this.config;
	}

	protected abstract Logger getLogger();
	protected abstract void createDirs() throws Exception;
	protected abstract void initOtherEntities(Object... otherEntities) throws Exception;

	public boolean isUserSchedulersAllowed()
	{
		return config.getAutomation().isUserSchedulersAllowed();
	}

	/*** "Files root" routines for absolute paths ***/
	
	public String getFilesRoot()
	{
		return root;
	}
	
	public boolean isRootOverridden()
	{
		return rootOverride;
	}
	
	public String getRootRelative(String fileName)
	{
		return getRootRelative(fileName, true);
	}

	public String getRootRelative(String fileName, boolean useReplacedPath)
	{
		if (useReplacedPath)
			fileName = getNewPath(fileName);
		if (new File(fileName).isAbsolute())
			return fileName;
		else
			return root+fileName;
	}

	public String getAppRoot()
	{
		return approot;
	}
	
	public String getAppRootRelative(String fileName)
	{
		return getAppRootRelative(fileName, true);
	}

	public String getAppRootRelative(String fileName, boolean useReplacedPath)
	{
		if (useReplacedPath)
			fileName = getNewPath(fileName);
		if (new File(fileName).isAbsolute())
			return fileName;
		else
			return approot+fileName;
	}
	
	public String excludeRoot(String fileName)
	{
		fileName = getNewPath(fileName);
		if (fileName.startsWith(root))
			return fileName.substring(root.length());
		else
			return fileName;
	}

	
	/*** Shorteners for general config directories ***/
	
	public String getSchedulersPath()
	{
		return getRootRelative(configFiles.getSchedulersDir());
	}
	
	public String getUsersListPath()
	{
		return getRootRelative(configFiles.getUsersFileName());
	}
	
	public String getUserSettingsPath()
	{
		return getRootRelative(configFiles.getUserSettingsDir());
	}
	
	public String getDictsPath()
	{
		return getRootRelative(configFiles.getDictsDir());
	}
	
	public String getLogsPath()
	{
		return getRootRelative(logsDir);
	}
	
	public boolean isLogsPathOverridden()
	{
		return logsOverride;
	}
	
	public String getConnectionsPath()
	{
		return getRootRelative(configFiles.getConnectionsDir());
	}

	public String getFileConnectionsPath()
	{
		return getRootRelative(configFiles.getFileConnectionsDir());
	}

	public String getDbConnectionsPath()
	{
		return getRootRelative(configFiles.getDbConnectionsDir());
	}

	public String getFtpConnectionsPath()
	{
		return getRootRelative(configFiles.getFtpConnectionsDir());
	}

	public String getHtmlTemplatesPath ()
	{
		return getRootRelative(configFiles.getHtmlTemplatesDir());
	}

	public String getTemplatesPath()
	{
		return getRootRelative(configFiles.getTemplatesDir());
	}
	
	public String getAutomationStoragePath()
	{
		return getRootRelative(configFiles.getAutomationStorageDir());
	}
	
	public String getScriptsPath()
	{
		return getRootRelative(configFiles.getScriptsDir());
	}
	
	public String getReportsPath()
	{
		return getRootRelative(configFiles.getReportsDir());
	}

	public String getLastExecutionPath()
	{
		return getRootRelative(configFiles.getLastExecutionDir());
	}

	public String getReportsFilePath()
	{
		return getAppRootRelative(configFiles.getReportFilesDir());
	}

	public String getOriginalReportsFilePath()
	{
		return getAppRootRelative(configFiles.getReportFilesDir(), false);
	}
	
	public String getRealTimeReportPath()
	{
		return getAppRootRelative(configFiles.getRealTimeReportDir());
	}

	public String getSchedulerInfoFilePath()
	{
		return getAppRootRelative(configFiles.getSchedulerInfoFilesDir());
	}

	public String getUploadStoragePath()
	{
		return getRootRelative(configFiles.getUploadStorageDir());
	}

	public String getTempDirPath()
	{
		return getRootRelative(configFiles.getTempDir());
	}

	public String getDataDirPath()
	{
		return getRootRelative(configFiles.getDataDir());
	}

	protected String getNewPath(String fileName)
	{
		if (replacedLocations == null)
			return fileName;
		return replacedLocations.getOrDefault(fileName, fileName);
	}
	
	protected SchedulersManager createSchedulersManager(String schedulersDir, String schedulersCfgFilePath) throws Exception
	{
		return new SchedulersManager(schedulerFactory, schedulersDir, schedulersCfgFilePath);
	}
	
	protected void initSchedulersManager() throws Exception
	{
		schedulersManager.loadSchedulers();
	}
	
	
	public ValueGenerator getCommonGenerator()
	{
		return valueGenerators.getCommonGenerator();
	}

	public ValueGenerators getValueGenerators()
	{
		return valueGenerators;
	}
	
	
	public SchedulerFactory getSchedulerFactory()
	{
		return schedulerFactory;
	}
	
	public SchedulersManager getSchedulersManager()
	{
		return schedulersManager;
	}


	public String getSaltedText(String source)
	{
		return source + getSalt();
	}
	
	protected String getSalt()
	{
		return ClearThCore.getDefaultSalt();
	}
	
	public static String getDefaultSalt()
	{
		return "E0RmFa2W8cm8QBa8NA7Ya0YcftsvDDoeHS53yLTXaw8=";
	}
	
	public static Connection createDBConnection(String url, String user, String password) throws SQLException
	{
		return createDBConnection(url, user, password, null);
	}

	public static Connection createDBConnection(String url, String user, String password, GlobalContext globalContext)
			throws SQLException
	{
		return new ClearThDbConnection(DriverManager.getConnection(url, user, password), globalContext);
	}

	public CodecsStorage getCodecs()
	{
		return codecs;
	}

	public Map<String, XmlMessageConverterConfig> getMessageConverterConfigs()
	{
		return messageConverterConfigs;
	}
	
	public Map<String, XmlScriptConverterConfig> getScriptConverterConfigs()
	{
		return scriptConverterConfigs;
	}

	public Map<String, XmlMessageHelperConfig> getMessageHelpers()
	{
		return messageHelpers;
	}

	public ConfigFiles getConfigFiles()
	{
		return configFiles;
	}

	public ClearThVersion getVersion()
	{
		return cthVersion;
	}


	public boolean isApiAuthRequired()
	{
		return true;
	}
	
	public UsersManager getUsersManager()
	{
		return usersManager;
	}
	
	public ActionFactory getActionFactory()
	{
		return actionFactory;
	}
	
	public ICodecFactory getCodecFactory()
	{
		return codecFactory;
	}
	
	public ICodec createCodec(String codecName) throws SettingsException
	{
		XmlCodecConfig codecConfig = codecs.getCodecConfig(codecName);

		if (codecConfig == null)
			throw new SettingsException("Codec '"+codecName+"' is not defined");
		
		try
		{
			return getCodecFactory().createCodec(codecConfig);
		}
		catch (Exception e)
		{
			throw new SettingsException("Could not create codec '"+codecName+"'", e);
		}
	}

	public ReportTemplatesProcessor getReportTemplatesProcessor() {
		return reportTemplatesProcessor;
	}

	public SchedulerInfoTemplatesProcessor getSchedulerInfoTemplatesProcessor() {
		return schedulerInfoTemplatesProcessor;
	}

	/**
	 *
	 * @return unique parameters according to the specific implementation, which will be added to 'report.ftl' template
	 */
	public abstract Map<String, Object> getAdditionalTemplateParams();

	
	public ClearThConnectionStorage getConnectionStorage()
	{
		return connectionStorage;
	}

	public SchedulerSettingsTransmitter getSchedulerSettingsTransmitter()
	{
		return schedulerSettingsTransmitter;
	}

	public SchedulerInfoExporter getSchedulerInfoExporter()
	{
		return schedulerInfoExporter;
	}

	public ConnectionsTransmitter getConnectionsTransmitter()
	{
		return connectionsTransmitter;
	}

	public void beforeShutdown()
	{
		connectionStorage.stopAllConnections();
		memoryMonitor.halt();
		Utils.closeResource(dataHandlersFactory);
	}

	
	public ToolsManager getToolsManager()
	{
		return toolsManager;
	}
	
	public ToolsFactory getToolsFactory()
	{
		return getToolsManager().getToolsFactory();
	}
	
	public MatrixDataFactory getMatrixDataFactory()
	{
		return matrixDataFactory;
	}

	public MvelVariablesFactory getMvelVariablesFactory()
	{
		return mvelVariablesFactory;
	}

	public ComparisonUtils getComparisonUtils() {
		return comparisonUtils;
	}

	public static ComparisonUtils comparisonUtils()
	{
		return instance.getComparisonUtils();
	}
	
	
	public Class<MatrixFunctions> getMatrixFunctionsClass()
	{
		return matrixFunctionsFactory.getRealClass();
	}

	public MatrixFunctions createMatrixFunctions(Map<String, Boolean> holidays,
												 Date businessDay,
												 Date baseTime,
												 boolean weekendHoliday,
												 ValueGenerator valueGenerator)
	{
		return matrixFunctionsFactory.createMatrixFunctions(holidays, businessDay, baseTime, weekendHoliday, valueGenerator);
	}

	public MatrixFunctions createMatrixFunctions(Map<String, Boolean> holidays, Date businessDay, Date baseTime, boolean weekendHoliday)
	{
		return matrixFunctionsFactory.createMatrixFunctions(holidays, businessDay, baseTime, weekendHoliday, getSchedulerFactory());
	}

	public MatrixFunctions createMatrixFunctions(Scheduler scheduler)
	{
		return matrixFunctionsFactory.createMatrixFunctions(scheduler, getSchedulerFactory());
	}
	
	public DataHandlersFactory getDataHandlersFactory()
	{
		return dataHandlersFactory;
	}
	
	
	public String getRepositoryPath()
	{
		return "";
	}

}
