package com.dynatrace.diagnostics.plugin.Solr.config;

import java.util.List;

public class Core {
	private String name;
	private List<String> queryHandlers;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getQueryHandlers() {
		return queryHandlers;
	}

	public void setQueryHandlers(List<String> queryHandlers) {
		this.queryHandlers = queryHandlers;
	}
}
