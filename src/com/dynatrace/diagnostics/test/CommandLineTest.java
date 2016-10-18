 /**
  * This class is used to test the monitor from the command line.
  **/ 
package com.dynatrace.diagnostics.test;

import com.dynatrace.diagnostics.plugin.solr.SolrRemote;

import com.dynatrace.diagnostics.pdk.Monitor;
import com.dynatrace.diagnostics.pdk.MonitorMeasure;
import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.Status;

import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

public class CommandLineTest {
	private static final Logger log = Logger.getLogger(CommandLineTest.class.getName());
	
	public static void main(String[] args) throws Exception {
		log.info("Starting Monitor Test");

		CommandLineEnvironment monitorEnvironment = new CommandLineEnvironment();
		monitorEnvironment.setConfigString("port", "8983");
		//monitorEnvironment.setConfigString("handlers", "/search;/browse;/dataimport");
		monitorEnvironment.setConfigString("handlers", "/dataimport");
		monitorEnvironment.setConfigString("protocol", "http");
		

		log.info("Begin Envionment Values:");
		log.info("Port: " + monitorEnvironment.getConfigString("port"));
		log.info("Handlers: " + monitorEnvironment.getConfigString("handlers"));
		log.info("Protocol: " + monitorEnvironment.getConfigString("protocol"));
		log.info("End Envionment Values:");
		
		String metricGroup = null;
/*
		metricGroup = "Cache Stats";
		monitorEnvironment.setMonitorMeasures(metricGroup,"Query Result Cache Hit Ratio");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Query Result Cache Hit Ratio Cumulative");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Query Result Cache Size");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Document Cache Hit Ratio");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Document Cache Hit Ratio Cumulative");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Document Cache Size");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Field Value Cache Hit Ratio");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Field Value Cache Hit Ratio Cumulative");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Field Value Cache Size");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Filter Cache Hit Ratio");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Filter Cache Hit Ratio Cumulative");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Filter Cache Size");

		metricGroup = "Core Stats";
		monitorEnvironment.setMonitorMeasures(metricGroup,"Num Docs");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Max Docs");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Deleted Docs");

		metricGroup = "Memory Stats";
		monitorEnvironment.setMonitorMeasures(metricGroup,"JVM Memory Used");
		monitorEnvironment.setMonitorMeasures(metricGroup,"JVM Memory Free");
		monitorEnvironment.setMonitorMeasures(metricGroup,"JVM Memory Total");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Free Physical Memory Size");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Total Physical Memory Size");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Committed Virtual Memory Size");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Free Swap Space Size");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Total Swap Space Size");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Open File Descriptor Count");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Max File Descriptor Count");

		metricGroup = "Query Stats";
		monitorEnvironment.setMonitorMeasures(metricGroup,"Requests");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Errors");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Timeouts");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Avg Requests");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Avg Time Per Request");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Five Min Rate Requests");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Five Min Rate Requests Per Second");
*/
		metricGroup = "Data Import Stats";
		monitorEnvironment.setMonitorMeasures(metricGroup,"Time Taken");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Total Requests made to DataSource");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Total Rows Fetched");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Total Documents Processed");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Total Documents Skipped");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Documents Added/Updated");
		monitorEnvironment.setMonitorMeasures(metricGroup,"Documents Deleted");

		Monitor monitor = new SolrRemote();
		
		log.info("Calling Monitor setup");
		monitor.setup(monitorEnvironment);

		log.info("Calling Monitor execute");
		monitor.execute(monitorEnvironment);

		log.info("Calling Monitor teardown");
		monitor.teardown(monitorEnvironment);
		
		log.info("Dumping Monitor Environment");
		monitorEnvironment.dump();

		log.info("Ending Monitor Test");
	}
}
