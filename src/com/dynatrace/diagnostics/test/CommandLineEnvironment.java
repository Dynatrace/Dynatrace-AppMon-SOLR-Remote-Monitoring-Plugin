 /**
  * This class is used to test the monitor from the command line.
  **/ 
package com.dynatrace.diagnostics.test;

import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.MonitorMeasure;

import java.io.File;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

public class CommandLineEnvironment implements MonitorEnvironment {
	private HashMap<String, String> configStrings = new HashMap<String, String>();
	private HashMap<String, Boolean> configBooleans = new HashMap<String, Boolean>();
	private HashMap<String, Date> configDates = new HashMap<String, Date>();
	private HashMap<String, Double> configDoubles = new HashMap<String, Double>();
	private HashMap<String, File> configFiles = new HashMap<String, File>();
	private HashMap<String, Long> configLongs = new HashMap<String, Long>();
	private HashMap<String, URL> configURLs = new HashMap<String, URL>();
	private HashMap<String, MonitorMeasureImpl> monitorMeasures = new HashMap<String, MonitorMeasureImpl>();
	private HashMap<String, MonitorMeasureImpl> monitorChildMeasures = new HashMap<String, MonitorMeasureImpl>();
	private ArrayList monitorMeasureList = new ArrayList();

	private static final Logger log = Logger.getLogger(CommandLineEnvironment.class.getName());
	
	public Collection getMonitorMeasures(String metricGroup, String metric) {
		MonitorMeasureImpl monitorMeasure = monitorMeasures.get(metricGroup + "|" + metric);
		ArrayList arrayList = new ArrayList(1);
		arrayList.add(monitorMeasure);
		return arrayList;
	}

    public Collection getMonitorMeasures() {return monitorMeasureList;}

    public MonitorMeasure createDynamicMeasure(MonitorMeasure monitormeasure, String splitName, String splitValue) {
		MonitorMeasureImpl monitorChildMeasure = new MonitorMeasureImpl();
		monitorChildMeasures.put(monitormeasure.getMetricGroupName() + "|" + monitormeasure.getMetricName() + "|" + splitName + "|" + splitValue,monitorChildMeasure);
		return monitorChildMeasure;
	}
	
    public Boolean getConfigBoolean(String s){return null;}

    public Date getConfigDate(String s) {return null;}

    public Double getConfigDouble(String s) {return null;}

    public File getConfigFile(String s) {return null;}

    public Long getConfigLong(String name) {
		return null;
	}

    public String getConfigString(String name) {
		return configStrings.get(name);
	}

    public String getConfigPassword(String s){return null;}

    public HostImpl getHost() {return new HostImpl();}

    public URL getConfigUrl(String s) {return null;}

    public boolean isStopped() {return false;}

    //The methods below are not part of the MonitorEnvironment Interface...
	
    public void setConfigString(String name, String value) {
		configStrings.put(name, value);
	}

    public void setConfig(String name, String value) {
		configStrings.put(name,value);
	}

    public void setConfig(String name, Boolean value) {
		configBooleans.put(name,value);
	}

    public void setConfig(String name, Date value) {
		configDates.put(name,value);
	}

    public void setConfig(String name, Double value) {
		configDoubles.put(name,value);
	}

    public void setConfig(String name, File value) {
		configFiles.put(name,value);
	}

    public void setConfig(String name, Long value) {
		configLongs.put(name,value);
	}

    public void setConfig(String name, URL value) {
		configURLs.put(name,value);
	}

    public void setMonitorMeasures(String metricGroup, String metric) {
		MonitorMeasureImpl monitorMeasure = new MonitorMeasureImpl();
		monitorMeasure.setMetricGroupName(metricGroup);
		monitorMeasure.setMetricName(metric);
		monitorMeasures.put(metricGroup + "|" + metric,monitorMeasure);
	}

    public MonitorMeasureImpl getDynamicMeasure(MonitorMeasure monitormeasure, String splitName, String splitValue) {
		return monitorChildMeasures.get(monitormeasure.getMetricGroupName() + "|" + monitormeasure.getMetricName() + "|" + splitName + "|" + splitValue);
	}

    public void dump() {
		log.info("Contents of CommandLineEnvironment:");
		log.info("Contents of CommandLineEnvironment configStrings:");
		log.info(configStrings.toString());
		log.info("Contents of CommandLineEnvironment monitorMeasures:");
		log.info(monitorMeasures.toString());
		log.info("Contents of CommandLineEnvironment monitorChildMeasures:");
		log.info(monitorChildMeasures.toString());
	}
}

