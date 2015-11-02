package com.dynatrace.diagnostics.plugin.Solr;

import com.dynatrace.diagnostics.plugin.Solr.config.Configuration;
import com.dynatrace.diagnostics.plugin.Solr.config.Core;
import com.dynatrace.diagnostics.plugin.Solr.config.Server;
import com.dynatrace.diagnostics.plugin.Solr.stats.CacheStats;
import com.dynatrace.diagnostics.plugin.Solr.stats.CoreStats;
import com.dynatrace.diagnostics.plugin.Solr.stats.MemoryStats;
import com.dynatrace.diagnostics.plugin.Solr.stats.QueryStats;
import com.dynatrace.diagnostics.pdk.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

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
	
	private String context_root;
	private static final String CORE_URI = "/admin/cores?action=STATUS&wt=json";
	private static String plugins_uri = "/%s/admin/plugins?wt=json";
	private static String memory_uri = "/%s/admin/system?stats=true&wt=json";
	private static String mbeansUri = "/%s/admin/mbeans?stats=true&wt=json";
	private String host;
	
	CloseableHttpClient httpclient;
	SolrHelper helper;

	private static final Logger log = Logger.getLogger(SolrRemote.class.getName());
	public Status setup(MonitorEnvironment env) throws Exception {

		return new Status(Status.StatusCode.Success);
	}

	public Status execute(MonitorEnvironment env) throws Exception {

		host = env.getHost().getAddress();
		int port = Integer.parseInt(env.getConfigString("port"));
		List<String> queryHandlers = Arrays.asList(env.getConfigString("handlers").split(";"));

		context_root = "http://" + host + ":" + port + "/solr";		
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
		}catch(Exception e){
			log.severe("ERROR: "+ e.getMessage());
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
	 
	 public List<Core> getCores(SolrHelper helper, Configuration config) {
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
						System.out.println("Error retrieving mbeans info for core: " + core);
						log.severe("Error retrieving mbeans info for core: " + core);
						log.severe(e.getMessage());
					}

					try {
						CoreStats coreStats = new CoreStats();
						coreStats.populateStats(solrMBeansHandlersMap);
						
						log.info("Capturing core stats metrics for core: " + core);
						Iterables.get(env.getMonitorMeasures("Core Stats", "Num Docs"), 0).setValue(coreStats.getNumDocs());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures(	"Core Stats", "Num Docs"), 0),
										"Core", core).setValue(coreStats.getNumDocs());
						
						Iterables.get(env.getMonitorMeasures("Core Stats", "Max Docs"), 0).setValue(coreStats.getMaxDocs());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures(	"Core Stats", "Max Docs"), 0),
										"Core", core).setValue(coreStats.getMaxDocs());
						
						Iterables.get(env.getMonitorMeasures("Core Stats", "Deleted Docs"), 0).setValue(coreStats.getDeletedDocs());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures(	"Core Stats", "Deleted Docs"), 0),
										"Core", core).setValue(coreStats.getDeletedDocs());
	
						//System.out.println(core + " " + coreStats.getNumDocs());
						//log.info("CoreStats: " + core + " " + coreStats.getNumDocs());
					} catch (Exception e) {
						System.out.println("Error retrieving stats for core: " + core);
						log.severe("Error retrieving stats for core: " + core);
						log.severe(e.getMessage());
					}

					try {
						Double solrRequests = 0.0;
						Double solrErrors = 0.0;
						Double solrTimeouts = 0.0;
						Double solrAvgRequests = 0.0;
						Double solrAvgTimePerRequest = 0.0;
						Double solrCountAvgTimePerRequest = 0.0;
						Double solrFiveMinRateRequests = 0.0;
						
						for (String handler : coreConfig.getQueryHandlers()) {
							QueryStats queryStats = new QueryStats();
							queryStats.populateStats(solrMBeansHandlersMap, handler);
							
							if (queryStats.getCoreExists()) {
							
								Double actualValue = 0.0;
							
								log.info("Capturing query stats metrics for core: " + core + " handler: " + handler);

								actualValue = calculateDifference("requests", queryStats.getRequests(), host + "_" + core + "_" + handler);
								solrRequests += actualValue;
								Iterables.get(env.getMonitorMeasures("Query Stats", "Requests"), 0).setValue(solrRequests);
								env.createDynamicMeasure(Iterables.get(
										env.getMonitorMeasures(
												"Query Stats", "Requests"), 0),
												"Handler", handler + " " + core).setValue(actualValue);
								
								actualValue = calculateDifference("errors", queryStats.getErrors(), host + "_" + core + "_" + handler);
								solrErrors  += actualValue;
								Iterables.get(env.getMonitorMeasures("Query Stats", "Errors"), 0).setValue(solrErrors);
								env.createDynamicMeasure(Iterables.get(
										env.getMonitorMeasures(
												"Query Stats", "Errors"), 0),
												"Handler", handler + " " + core).setValue(actualValue);
								
								actualValue = calculateDifference("timeouts", queryStats.getTimeouts(), host + "_" + core + "_" + handler);
								solrTimeouts += actualValue;
								Iterables.get(env.getMonitorMeasures("Query Stats", "Timeouts"), 0).setValue(solrTimeouts);
								env.createDynamicMeasure(Iterables.get(
										env.getMonitorMeasures(
												"Query Stats", "Timeouts"), 0),
												"Handler", handler + " " + core).setValue(
												queryStats.getTimeouts());
								
								solrAvgRequests += queryStats.getAvgRequests();
								Iterables.get(env.getMonitorMeasures("Query Stats", "Avg Requests"), 0).setValue(solrAvgRequests);
								env.createDynamicMeasure(Iterables.get(
										env.getMonitorMeasures(
												"Query Stats", "Avg Requests"), 0),
												"Handler", handler + " " + core).setValue(
												queryStats.getAvgRequests());
								
								solrCountAvgTimePerRequest += 1.0;
								solrAvgTimePerRequest += queryStats.getAvgTimePerRequest();
								Iterables.get(env.getMonitorMeasures("Query Stats", "Avg Time Per Request"), 0).setValue(solrAvgTimePerRequest / solrCountAvgTimePerRequest);
								env.createDynamicMeasure(Iterables.get(
										env.getMonitorMeasures(
												"Query Stats", "Avg Time Per Request"), 0),
												"Handler", handler + " " + core).setValue(
												queryStats.getAvgTimePerRequest());
								
								solrFiveMinRateRequests += queryStats.getFiveMinRateRequests();
								Iterables.get(env.getMonitorMeasures("Query Stats", "Five Min Rate Requests"), 0).setValue(solrFiveMinRateRequests);
								env.createDynamicMeasure(Iterables.get(
										env.getMonitorMeasures(
												"Query Stats", "Five Min Rate Requests"), 0),
												"Handler", handler + " " + core).setValue(
												queryStats.getFiveMinRateRequests());
							}
						}

					} catch (Exception e) {
						System.out.println("Error retrieving query stats for core: " + core);
						log.severe("Error retrieving query stats for core: " + core);
						log.severe(e.getMessage());
					}

					try {
						CacheStats cacheStats = new CacheStats();
						cacheStats.populateStats(solrMBeansHandlersMap);
						//System.out.println("Cache: " + core + " " + cacheStats.getDocumentCacheHitRatio());
						log.info("Capturing cache status metrics for core: " + core);
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Query Result Cache Hit Ratio"), 0).setValue(cacheStats.getQueryResultCacheHitRatio());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Query Result Cache Hit Ratio"), 0),
										"Core", core).setValue(cacheStats.getQueryResultCacheHitRatio());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Query Result Cache Hit Ratio Cumulative"),
								0).setValue(cacheStats.getQueryResultCacheHitRatioCumulative());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Query Result Cache Hit Ratio Cumulative"), 0),
										"Core", core).setValue(cacheStats.getQueryResultCacheHitRatioCumulative());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Query Result Cache Size"),
								0).setValue(cacheStats.getQueryResultCacheSize());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Query Result Cache Size"), 0),
										"Core", core).setValue(cacheStats.getQueryResultCacheSize());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Document Cache Hit Ratio"),
								0).setValue(cacheStats.getDocumentCacheHitRatio());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Document Cache Hit Ratio"), 0),
										"Core", core).setValue(cacheStats.getDocumentCacheHitRatio());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Document Cache Hit Ratio Cumulative"),
								0).setValue(cacheStats.getDocumentCacheHitRatioCumulative());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Document Cache Hit Ratio Cumulative"), 0),
										"Core", core).setValue(cacheStats.getDocumentCacheHitRatioCumulative());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Document Cache Size"),
								0).setValue(cacheStats.getDocumentCacheSize());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Document Cache Size"), 0),
										"Core", core).setValue(cacheStats.getDocumentCacheSize());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Field Value Cache Hit Ratio"),
								0).setValue(cacheStats.getFieldValueCacheHitRatio());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Field Value Cache Hit Ratio"), 0),
										"Core", core).setValue(cacheStats.getFieldValueCacheHitRatio());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Field Value Cache Hit Ratio Cumulative"),
								0).setValue(cacheStats.getFieldValueCacheHitRatioCumulative());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Field Value Cache Hit Ratio Cumulative"), 0),
										"Core", core).setValue(cacheStats.getFieldValueCacheHitRatioCumulative());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Field Value Cache Size"),
								0).setValue(cacheStats.getFieldValueCacheSize());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Field Value Cache Size"), 0),
										"Core", core).setValue(cacheStats.getFieldValueCacheSize());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Filter Cache Hit Ratio"),
								0).setValue(cacheStats.getFilterCacheHitRatio());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Filter Cache Hit Ratio"), 0),
										"Core", core).setValue(cacheStats.getFilterCacheHitRatio());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Filter Cache Hit Ratio Cumulative"),
								0).setValue(cacheStats.getFilterCacheHitRatioCumulative());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Filter Cache Hit Ratio Cumulative"), 0),
										"Core", core).setValue(cacheStats.getFilterCacheHitRatioCumulative());
						
						Iterables.get(env.getMonitorMeasures("Cache Stats", "Filter Cache Size"),
								0).setValue(cacheStats.getFilterCacheSize());
						env.createDynamicMeasure(Iterables.get(
								env.getMonitorMeasures("Cache Stats", "Filter Cache Size"), 0),
										"Core", core).setValue(cacheStats.getFilterCacheSize());
						
						//log.info("Cache: " + core + " " + cacheStats.getDocumentCacheHitRatio());
					} catch (Exception e) {
						System.out.println("Error retrieving cache stats for core: " + core);
						log.severe("Error retrieving cache stats for core: " + core);
						log.severe(e.getMessage());
					}
				}

				try {
					MemoryStats memoryStats = new MemoryStats();
					String uri = context_root + String.format(memory_uri, core);
					log.info("Capturing memory metrics for core: " + core);
					
					HttpGet request = new HttpGet(uri);
					HttpResponse response = httpClient.execute(request);
					InputStream inputStream = response.getEntity().getContent();
					
					memoryStats.populateStats(inputStream);

					Iterables.get(env.getMonitorMeasures("Memory Stats", "JVM Memory Used"), 0).setValue(memoryStats.getJvmMemoryUsed());
					Iterables.get(env.getMonitorMeasures("Memory Stats", "JVM Memory Free"), 0).setValue(memoryStats.getJvmMemoryFree());
					Iterables.get(env.getMonitorMeasures("Memory Stats", "JVM Memory Total"), 0).setValue(memoryStats.getJvmMemoryTotal());
					Iterables.get(env.getMonitorMeasures("Memory Stats", "Free Physical Memory Size"), 0).setValue(memoryStats.getFreePhysicalMemorySize());
					Iterables.get(env.getMonitorMeasures("Memory Stats", "Total Physical Memory Size"), 0).setValue(memoryStats.getTotalPhysicalMemorySize());
					Iterables.get(env.getMonitorMeasures("Memory Stats", "Committed Virtual Memory Size"), 0).setValue(memoryStats.getCommittedVirtualMemorySize());
					Iterables.get(env.getMonitorMeasures("Memory Stats", "Free Swap Space Size"), 0).setValue(memoryStats.getFreeSwapSpaceSize());
					Iterables.get(env.getMonitorMeasures("Memory Stats", "Total Swap Space Size"), 0).setValue(memoryStats.getTotalSwapSpaceSize());
					Iterables.get(env.getMonitorMeasures("Memory Stats", "Open File Descriptor Count"), 0).setValue(memoryStats.getOpenFileDescriptorCount());
					Iterables.get(env.getMonitorMeasures("Memory Stats", "Max File Descriptor Count"), 0).setValue(memoryStats.getMaxFileDescriptorCount());
					
					env.createDynamicMeasure(Iterables.get(env.getMonitorMeasures("Memory Stats", "JVM Memory Used"), 0), "Core", core).setValue(memoryStats.getJvmMemoryUsed());
					env.createDynamicMeasure(Iterables.get(env.getMonitorMeasures("Memory Stats", "JVM Memory Free"), 0), "Core", core).setValue(memoryStats.getJvmMemoryFree());
					env.createDynamicMeasure(Iterables.get(env.getMonitorMeasures("Memory Stats", "JVM Memory Total"), 0), "Core", core).setValue(memoryStats.getJvmMemoryTotal());
					env.createDynamicMeasure(Iterables.get(env.getMonitorMeasures("Memory Stats", "Free Physical Memory Size"), 0), "Core", core).setValue(memoryStats.getFreePhysicalMemorySize());
					env.createDynamicMeasure(Iterables.get(env.getMonitorMeasures("Memory Stats", "Total Physical Memory Size"), 0), "Core", core).setValue(memoryStats.getTotalPhysicalMemorySize());
					env.createDynamicMeasure(Iterables.get(env.getMonitorMeasures("Memory Stats", "Committed Virtual Memory Size"), 0), "Core", core).setValue(memoryStats.getCommittedVirtualMemorySize());
					env.createDynamicMeasure(Iterables.get(env.getMonitorMeasures("Memory Stats", "Free Swap Space Size"), 0), "Core", core).setValue(memoryStats.getFreeSwapSpaceSize());
					env.createDynamicMeasure(Iterables.get(env.getMonitorMeasures("Memory Stats", "Total Swap Space Size"), 0), "Core", core).setValue(memoryStats.getTotalSwapSpaceSize());
					env.createDynamicMeasure(Iterables.get(env.getMonitorMeasures("Memory Stats", "Open File Descriptor Count"), 0), "Core", core).setValue(memoryStats.getOpenFileDescriptorCount());
					env.createDynamicMeasure(Iterables.get(env.getMonitorMeasures("Memory Stats", "Max File Descriptor Count"), 0), "Core", core).setValue(memoryStats.getMaxFileDescriptorCount());
					
				} catch (Exception e) {
					System.out.println("Error retrieving memory stats for core: " + core);
					log.severe("Error retrieving memory stats for core: " + core);
					log.severe(e.getMessage());
					StackTraceElement[] sts = e.getStackTrace();
					for (StackTraceElement stackTraceElement : sts) {
						log.severe(stackTraceElement.toString());
					}
					e.printStackTrace();
				}
			}
		}
	    public double calculateDifference(String metric, double current, String identifier) throws Exception{
	    	//Dealing with CUMULATIVE METRICS
	    	File cummulativeFile = new File((metric +"_"+ identifier + ".txt").replace("/", ""));
	    	log.info("File name used for cumulative metrics: " + cummulativeFile);
	    	double actual = 0;
	    	double previous = 0;
	    	long unsignedPrevious;
	    	boolean isUnsigned = false;
	    	DecimalFormat format = new DecimalFormat();
	    	format.setDecimalSeparatorAlwaysShown(false);
	    	
			try{
				if(!cummulativeFile.exists()) {
					log.info("File does not exist, creating it now...");
				    cummulativeFile.createNewFile();
				}else{
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
						
						log.log(Level.SEVERE, e.getMessage(), e);
						e.printStackTrace();
						bufferedReader.close();
						throw e;
					}

				}else
					previous = current;
				if (current >= previous){
					actual = current - previous;
				}else{
					actual = 0;
				}

				bufferedReader.close();
				
				FileOutputStream oFile = new FileOutputStream(cummulativeFile, false); 
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(oFile));
				
				String valueAsString = format.format(current).replace(",", "");
				bw.write(valueAsString);
				bw.close();
				
			}catch (Exception e){
				e.printStackTrace();
				log.severe("\nPREVIOUS: " + previous +" \nCURRENT: " +current+ "\nACTUAL: " + actual );
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}
			return actual;
	    }
}
