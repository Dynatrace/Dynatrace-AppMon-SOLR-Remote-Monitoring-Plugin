package com.dynatrace.diagnostics.plugin.Solr.config;

import java.util.List;

public class Configuration {
	private Server server;
	private List<Core> cores;
	private String metricPrefix;

	public List<Core> getCores() {
		return cores;
	}

	public void setCores(List<Core> cores) {
		this.cores = cores;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		this.metricPrefix = metricPrefix;
	}
}
