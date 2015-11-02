package com.dynatrace.diagnostics.plugin.solr.stats;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class CoreStats {
	private Double numDocs;
	private Double maxDocs;
	private Double deletedDocs;

	public void populateStats(Map<String, JsonNode> solrMBeansHandlersMap) {
		JsonNode node = solrMBeansHandlersMap.get("CORE");
		if (node != null) {
			JsonNode coreNode = node.path("searcher").path("stats");
			if (!coreNode.isMissingNode()) {
				this.setNumDocs(coreNode.path("numDocs").asDouble());
				this.setMaxDocs(coreNode.path("maxDoc").asDouble());
				this.setDeletedDocs(coreNode.path("deletedDocs").asDouble());
			}
		}
	}

	public Double getNumDocs() {
		return numDocs;
	}

	public void setNumDocs(Double numDocs) {
		this.numDocs = numDocs;
	}

	public Double getMaxDocs() {
		return maxDocs;
	}

	public void setMaxDocs(Double maxDocs) {
		this.maxDocs = maxDocs;
	}

	public Double getDeletedDocs() {
		return deletedDocs;
	}

	public void setDeletedDocs(Double deletedDocs) {
		this.deletedDocs = deletedDocs;
	}
}
