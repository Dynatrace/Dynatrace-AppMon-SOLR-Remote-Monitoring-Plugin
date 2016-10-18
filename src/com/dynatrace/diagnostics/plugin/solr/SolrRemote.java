package com.dynatrace.diagnostics.plugin.solr;

import com.dynatrace.diagnostics.plugin.solr.config.Configuration;
import com.dynatrace.diagnostics.plugin.solr.config.Core;
import com.dynatrace.diagnostics.plugin.solr.config.Server;
import com.dynatrace.diagnostics.plugin.solr.stats.CacheStats;
import com.dynatrace.diagnostics.plugin.solr.stats.CoreStats;
import com.dynatrace.diagnostics.plugin.solr.stats.DataImportStats;
import com.dynatrace.diagnostics.plugin.solr.stats.MemoryStats;
import com.dynatrace.diagnostics.plugin.solr.stats.QueryStats;
import com.dynatrace.diagnostics.pdk.Monitor;
import com.dynatrace.diagnostics.pdk.MonitorMeasure;
import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.Status;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class SolrRemote implements Monitor {
	private static final String CORE_URI = "/admin/cores?action=STATUS&wt=json";
	private static String plugins_uri = "/%s/admin/plugins?wt=json";
	private static String memory_uri = "/%s/admin/system?stats=true&wt=json";
	private static String mbeansUri = "/%s/admin/mbeans?stats=true&wt=json";
	private static String dataImportUri = "/%s/dataimport?stats=true&wt=json";

	private String context_root;
	private String host;
	
	private CloseableHttpClient httpclient;

	private SolrHelper helper;

	private static final Logger log = Logger.getLogger(SolrRemote.class.getName());

	public Status setup(MonitorEnvironment env) throws Exception {
		return new Status(Status.StatusCode.Success);
	}

	public Status execute(MonitorEnvironment env) throws Exception {
		String protocol = env.getConfigString("protocol");
		host = env.getHost().getAddress();
		int port = Integer.parseInt(env.getConfigString("port"));
		List<String> queryHandlers = Arrays.asList(env.getConfigString("handlers").split(";"));

		context_root = protocol + "://" + host + ":" + port + "/solr";		
		httpclient = HttpClients.createDefault();
		
		Server server = new Server();
		server.setHost(host);
		server.setPort(port);

		log.info("Connecting to solr on host: " + host + " port: " + port);
		helper = new SolrHelper(httpclient);
		
		List<String> listCoresNames = helper.getCores("http://" + host +":" + port + "/solr/admin/cores?action=STATUS&wt=json");
		List<Core> listCores = new ArrayList<Core>();
		
		for (String core : listCoresNames) {
			log.info("Core: " + core + " was found");
			Core c = new Core();
			c.setName(core);
			c.setQueryHandlers(queryHandlers);
			listCores.add(c);
		}
		
		Configuration config = new Configuration();
		config.setCores(listCores);
		config.setServer(server);
	
		try{
			getSOLRMetrics(httpclient, helper, listCores, env);
		} catch (Exception e) {
			log.severe("Error retrieving SOLR metrics");
			log.severe(e.getMessage());
			StackTraceElement[] sts = e.getStackTrace();
			for (StackTraceElement stackTraceElement : sts) {
				log.severe(stackTraceElement.toString());
			}
		}
		return new Status(Status.StatusCode.Success);
	}

	public void teardown(MonitorEnvironment env) throws Exception {
		if (httpclient != null){
			httpclient.close();
		}
		if (helper != null){
			helper = null;
		}
	}
	 
	private List<Core> getCores(SolrHelper helper, Configuration config) {
		List<Core> cores = new ArrayList<Core>();
		if (config != null && config.getCores() != null) {
			cores = config.getCores();
		}
		
		Iterator<Core> iterator = cores.iterator();
		while (iterator.hasNext()) {
			if (Strings.isNullOrEmpty(iterator.next().getName())) {
				iterator.remove();
			}
		}
		return cores;
	}

	private void getSOLRMetrics(CloseableHttpClient httpClient,
			SolrHelper helper,
			List<Core> coresConfig,
			MonitorEnvironment env) throws IOException {
		
		for (Core coreConfig : coresConfig) {
			String core = coreConfig.getName();
			
			if (helper.checkIfMBeanHandlerSupported(String.format(context_root + plugins_uri, core))) {
				Map<String, JsonNode> solrMBeansHandlersMap = new HashMap<String, JsonNode>();
				try {
					solrMBeansHandlersMap = helper.getSolrMBeansHandlersMap(core, context_root + mbeansUri);
				} catch (Exception e) {
					log.severe("Error retrieving mbeans for core: " + core);
					log.severe(e.getMessage());
					StackTraceElement[] sts = e.getStackTrace();
					for (StackTraceElement stackTraceElement : sts) {
						log.severe(stackTraceElement.toString());
					}
				}

				try {
					CoreStats coreStats = new CoreStats();
					coreStats.populateStats(solrMBeansHandlersMap);
					
					log.info("Capturing core stats metrics for core: " + core);

					String metricGroup = "Core Stats";

					createDynamicMeasurePerCore(env, coreStats.getNumDocs(), metricGroup, "Num Docs", core);
					
					createDynamicMeasurePerCore(env, coreStats.getMaxDocs(), metricGroup, "Max Docs", core);
					
					createDynamicMeasurePerCore(env, coreStats.getDeletedDocs(), metricGroup, "Deleted Docs", core);
				} catch (Exception e) {
					log.severe("Error retrieving core status metrics for core: " + core);
					log.severe(e.getMessage());
					StackTraceElement[] sts = e.getStackTrace();
					for (StackTraceElement stackTraceElement : sts) {
						log.severe(stackTraceElement.toString());
					}
				}

				try {
					Double solrRequests = 0.0;
					Double solrErrors = 0.0;
					Double solrTimeouts = 0.0;
					Double solrAvgRequests = 0.0;
					Double solrAvgTimePerRequest = 0.0;
					Double solrCountAvgTimePerRequest = 0.0;
					Double solrFiveMinRateRequests = 0.0;
					Double solrFiveMinRateRequestsPerSecond = 0.0;
					
					for (String handler : coreConfig.getQueryHandlers()) {
						QueryStats queryStats = new QueryStats();
						queryStats.populateStats(solrMBeansHandlersMap, handler);
						
						if (queryStats.getCoreExists()) {
						
							Double actualValue = 0.0;
						
							log.info("Capturing query stats metrics for core: " + core + " handler: " + handler);

							String metricGroup = "Query Stats";

							actualValue = calculateDifference("requests", queryStats.getRequests(), host + "_" + core + "_" + handler);
							solrRequests += actualValue;
							setMeasure(env, solrRequests, metricGroup, "Requests");
							createDynamicMeasurePerHandlerCore(env, actualValue, metricGroup, "Requests", handler, core);
							
							actualValue = calculateDifference("errors", queryStats.getErrors(), host + "_" + core + "_" + handler);
							solrErrors  += actualValue;
							setMeasure(env, solrErrors, metricGroup, "Errors");
							createDynamicMeasurePerHandlerCore(env, actualValue, metricGroup, "Errors", handler, core);
							
							actualValue = calculateDifference("timeouts", queryStats.getTimeouts(), host + "_" + core + "_" + handler);
							solrTimeouts += actualValue;
							setMeasure(env, solrTimeouts, metricGroup, "Timeouts");
							createDynamicMeasurePerHandlerCore(env, queryStats.getTimeouts(), metricGroup, "Timeouts", handler, core);
							
							solrAvgRequests += queryStats.getAvgRequests();
							setMeasure(env, solrAvgRequests, metricGroup, "Avg Requests");
							createDynamicMeasurePerHandlerCore(env, queryStats.getAvgRequests(), metricGroup, "Avg Requests", handler, core);
							
							solrCountAvgTimePerRequest += 1.0;
							solrAvgTimePerRequest += queryStats.getAvgTimePerRequest();
							setMeasure(env, solrAvgTimePerRequest / solrCountAvgTimePerRequest, metricGroup, "Avg Time Per Request");
							createDynamicMeasurePerHandlerCore(env, queryStats.getAvgTimePerRequest(), metricGroup, "Avg Time Per Request", handler, core);
							
							solrFiveMinRateRequests += queryStats.getFiveMinRateRequests();
							setMeasure(env, solrFiveMinRateRequests, metricGroup, "Five Min Rate Requests");
							createDynamicMeasurePerHandlerCore(env, queryStats.getFiveMinRateRequests(), metricGroup, "Five Min Rate Requests", handler, core);

							solrFiveMinRateRequestsPerSecond += queryStats.getFiveMinRateRequestsPerSecond();
							setMeasure(env, solrFiveMinRateRequestsPerSecond, metricGroup, "Five Min Rate Requests Per Second");
							createDynamicMeasurePerHandlerCore(env, queryStats.getFiveMinRateRequestsPerSecond(), metricGroup, "Five Min Rate Requests Per Second", handler, core);
						}

					}

				} catch (Exception e) {
					log.severe("Error retrieving query status metrics for core: " + core);
					log.severe(e.getMessage());
					StackTraceElement[] sts = e.getStackTrace();
					for (StackTraceElement stackTraceElement : sts) {
						log.severe(stackTraceElement.toString());
					}
				}

				try {
					CacheStats cacheStats = new CacheStats();
					cacheStats.populateStats(solrMBeansHandlersMap);
					log.info("Capturing cache status metrics for core: " + core);
					
					String metricGroup = "Cache Stats";

					createDynamicMeasurePerCore(env, cacheStats.getQueryResultCacheHitRatio(), metricGroup, "Query Result Cache Hit Ratio", core);

					createDynamicMeasurePerCore(env, cacheStats.getQueryResultCacheHitRatioCumulative(), metricGroup, "Query Result Cache Hit Ratio Cumulative", core);
					
					createDynamicMeasurePerCore(env, cacheStats.getQueryResultCacheSize(), metricGroup, "Query Result Cache Size", core);
					
					createDynamicMeasurePerCore(env, cacheStats.getDocumentCacheHitRatio(), metricGroup, "Document Cache Hit Ratio", core);
					
					createDynamicMeasurePerCore(env, cacheStats.getDocumentCacheHitRatioCumulative(), metricGroup, "Document Cache Hit Ratio Cumulative", core);
					
					createDynamicMeasurePerCore(env, cacheStats.getDocumentCacheSize(), metricGroup, "Document Cache Size", core);
					
					createDynamicMeasurePerCore(env, cacheStats.getFieldValueCacheHitRatio(), metricGroup, "Field Value Cache Hit Ratio", core);
					
					createDynamicMeasurePerCore(env, cacheStats.getFieldValueCacheHitRatioCumulative(), metricGroup, "Field Value Cache Hit Ratio Cumulative", core);
					
					createDynamicMeasurePerCore(env, cacheStats.getFieldValueCacheSize(), metricGroup, "Field Value Cache Size", core);
					
					createDynamicMeasurePerCore(env, cacheStats.getFilterCacheHitRatio(), metricGroup, "Filter Cache Hit Ratio", core);
					
					createDynamicMeasurePerCore(env, cacheStats.getFilterCacheHitRatioCumulative(), metricGroup, "Filter Cache Hit Ratio Cumulative", core);
					
					createDynamicMeasurePerCore(env, cacheStats.getFilterCacheSize(), metricGroup, "Filter Cache Size", core);
				} catch (Exception e) {
					log.severe("Error retrieving cache status metrics for core: " + core);
					log.severe(e.getMessage());
					StackTraceElement[] sts = e.getStackTrace();
					for (StackTraceElement stackTraceElement : sts) {
						log.severe(stackTraceElement.toString());
					}
				}
			}

			try {
				MemoryStats memoryStats = new MemoryStats();
				String uri = context_root + String.format(memory_uri, core);
				log.info("Capturing memory status metrics for core: " + core);
				
				HttpGet request = new HttpGet(uri);
				HttpResponse response = httpClient.execute(request);
				InputStream inputStream = response.getEntity().getContent();
				
				memoryStats.populateStats(inputStream);

				String metricGroup = "Memory Stats";

				createDynamicMeasurePerCore(env, memoryStats.getJvmMemoryUsed(), metricGroup, "JVM Memory Used", core);

				createDynamicMeasurePerCore(env, memoryStats.getJvmMemoryFree(), metricGroup, "JVM Memory Free", core);
				
				createDynamicMeasurePerCore(env, memoryStats.getJvmMemoryTotal(), metricGroup, "JVM Memory Total", core);
				
				createDynamicMeasurePerCore(env, memoryStats.getFreePhysicalMemorySize(), metricGroup, "Free Physical Memory Size", core);
				
				createDynamicMeasurePerCore(env, memoryStats.getTotalPhysicalMemorySize(), metricGroup, "Total Physical Memory Size", core);
				
				createDynamicMeasurePerCore(env, memoryStats.getCommittedVirtualMemorySize(), metricGroup, "Committed Virtual Memory Size", core);
				
				createDynamicMeasurePerCore(env, memoryStats.getFreeSwapSpaceSize(), metricGroup, "Free Swap Space Size", core);
				
				createDynamicMeasurePerCore(env, memoryStats.getTotalSwapSpaceSize(), metricGroup, "Total Swap Space Size", core);
				
				createDynamicMeasurePerCore(env, memoryStats.getOpenFileDescriptorCount(), metricGroup, "Open File Descriptor Count", core);
				
				createDynamicMeasurePerCore(env, memoryStats.getMaxFileDescriptorCount(), metricGroup, "Max File Descriptor Count", core);
				
			} catch (Exception e) {
				log.severe("Error retrieving memory status metrics for core: " + core);
				log.severe(e.getMessage());
				StackTraceElement[] sts = e.getStackTrace();
				for (StackTraceElement stackTraceElement : sts) {
					log.severe(stackTraceElement.toString());
				}
			}

			if (coreConfig.getQueryHandlers().contains("/dataimport")) {
				try {
					log.info("Capturing data import status metrics for core: " + core);
					
					Map<String, JsonNode> dataImportStatusMap = new HashMap<String, JsonNode>();
					dataImportStatusMap = helper.getDataImportStatusMap(core, context_root + dataImportUri);

					DataImportStats dataImportStats = new DataImportStats();
					dataImportStats.populateStats(dataImportStatusMap);
					
					String metricGroup = "Data Import Stats";

					createDynamicMeasurePerCore(env, dataImportStats.getTotalRequestsMadeToDataSource(), metricGroup, "Total Requests made to DataSource", core);

					createDynamicMeasurePerCore(env, dataImportStats.getTotalRowsFetched(), metricGroup, "Total Rows Fetched", core);

					createDynamicMeasurePerCore(env, dataImportStats.getTotalDocumentsProcessed(), metricGroup, "Total Documents Processed", core);

					createDynamicMeasurePerCore(env, dataImportStats.getTotalDocumentsSkipped(), metricGroup, "Total Documents Skipped", core);

					createDynamicMeasurePerCore(env, dataImportStats.getTimeTaken(), metricGroup, "Time Taken", core);

					createDynamicMeasurePerCore(env, dataImportStats.getDocumentsAddedUpdated(), metricGroup, "Documents Added/Updated", core);

					createDynamicMeasurePerCore(env, dataImportStats.getDocumentsDeleted(), metricGroup, "Documents Deleted", core);
				} catch (Exception e) {
					log.severe("Error retrieving data import status metrics for core: " + core);
					log.severe(e.getMessage());
					StackTraceElement[] sts = e.getStackTrace();
					for (StackTraceElement stackTraceElement : sts) {
						log.severe(stackTraceElement.toString());
					}
				}
			}
		}
	}
		
	private double calculateDifference(String metric, double current, String identifier) throws Exception {
		//Dealing with CUMULATIVE METRICS
		File cummulativeFile = new File((metric +"_"+ identifier + ".txt").replace("/", ""));
		log.info("File name used for cumulative metrics: " + cummulativeFile);
		double actual = 0;
		double previous = 0;
		long unsignedPrevious;
		boolean isUnsigned = false;
		DecimalFormat format = new DecimalFormat();
		format.setDecimalSeparatorAlwaysShown(false);
		
		try {
			if(!cummulativeFile.exists()) {
				log.info("File does not exist, creating it now...");
				cummulativeFile.createNewFile();
			} else {
				log.info("File already exists...");
			}
			BufferedReader bufferedReader = new BufferedReader(new FileReader(cummulativeFile));
			String lineRead = bufferedReader.readLine();

			if(lineRead != null){
				if (lineRead.charAt(0) == '-'){
					isUnsigned = true;
				}
				try {
					if (isUnsigned){
						unsignedPrevious = Integer.parseInt(lineRead) & 0x00000000ffffffffL;
						previous = unsignedPrevious;
					}
					else{
						previous = Double.valueOf(lineRead);
					}
					
				} catch (Exception e) {
					previous = current;
					log.severe("Error calculating difference for metric: " + metric + " identifier: " + identifier);
					log.severe(e.getMessage());
					StackTraceElement[] sts = e.getStackTrace();
					for (StackTraceElement stackTraceElement : sts) {
						log.severe(stackTraceElement.toString());
					}
					bufferedReader.close();
					throw e;
				}

			} else {
				previous = current;
			}
			if (current >= previous){
				actual = current - previous;
			} else {
				actual = 0;
			}

			bufferedReader.close();
			
			FileOutputStream oFile = new FileOutputStream(cummulativeFile, false); 
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(oFile));
			
			String valueAsString = format.format(current).replace(",", "");
			bw.write(valueAsString);
			bw.close();
			
		} catch (Exception e){
			log.severe("\nPREVIOUS: " + previous +" \nCURRENT: " +current+ "\nACTUAL: " + actual );
			log.severe(e.getMessage());
			StackTraceElement[] sts = e.getStackTrace();
			for (StackTraceElement stackTraceElement : sts) {
				log.severe(stackTraceElement.toString());
			}
			throw e;
		}
		return actual;
	}
	
	private void createDynamicMeasurePerCore(MonitorEnvironment env, Double value, String metricGroup, String metric, String core) {
		if (value != null) {
			Collection<MonitorMeasure> monitorMeasures = env.getMonitorMeasures(metricGroup, metric);
			for (MonitorMeasure subscribedMonitorMeasure : monitorMeasures) {
				if (subscribedMonitorMeasure != null) {
					MonitorMeasure dynamicMeasure = env.createDynamicMeasure(subscribedMonitorMeasure, "Core", core);
					dynamicMeasure.setValue(value);
				}
			}
		}
	}

	private void createDynamicMeasurePerHandlerCore(MonitorEnvironment env, Double value, String metricGroup, String metric, String handler, String core) {
		if (value != null) {
			Collection<MonitorMeasure> monitorMeasures = env.getMonitorMeasures(metricGroup, metric);
			for (MonitorMeasure subscribedMonitorMeasure : monitorMeasures) {
				if (subscribedMonitorMeasure != null) {
					MonitorMeasure dynamicMeasure = env.createDynamicMeasure(subscribedMonitorMeasure, "Handler", handler + " " + core);
					dynamicMeasure.setValue(value);
				}
			}
		}
	}

	private void setMeasure(MonitorEnvironment env, Double value, String metricGroup, String metric) {
		if (value != null) {
			Collection<MonitorMeasure> monitorMeasures = env.getMonitorMeasures(metricGroup, metric);
			for (MonitorMeasure subscribedMonitorMeasure : monitorMeasures) {
				if (subscribedMonitorMeasure != null) {
					subscribedMonitorMeasure.setValue(value);
				}
			}
		}
	}
}
