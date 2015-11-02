package com.dynatrace.diagnostics.plugin.solr.stats;

import java.util.Map;

import com.dynatrace.diagnostics.plugin.solr.SolrHelper;

import com.fasterxml.jackson.databind.JsonNode;

public class CacheStats {

	private static final int PERCENT_MULTIPLIER = 100;

	private Double queryResultCacheHitRatio;
	private Double queryResultCacheHitRatioCumulative;
	private Double queryResultCacheSize;

	private Double documentCacheHitRatio;
	private Double documentCacheHitRatioCumulative;
	private Double documentCacheSize;

	private Double fieldValueCacheHitRatio;
	private Double fieldValueCacheHitRatioCumulative;
	private Double fieldValueCacheSize;

	private Double filterCacheHitRatio;
	private Double filterCacheHitRatioCumulative;
	private Double filterCacheSize;

	public void populateStats(Map<String, JsonNode> solrMBeansHandlersMap) throws Exception {

		JsonNode cacheNode = solrMBeansHandlersMap.get("CACHE");
		JsonNode queryResultCacheStats = cacheNode.path("queryResultCache").path("stats");

		if (!queryResultCacheStats.isMissingNode()) {
			this.setQueryResultCacheHitRatio(SolrHelper.multipyBy(queryResultCacheStats.path("hitratio").asDouble(), PERCENT_MULTIPLIER));
			this.setQueryResultCacheHitRatioCumulative(SolrHelper.multipyBy(queryResultCacheStats.path("cumulative_hitratio").asDouble(), PERCENT_MULTIPLIER));
			this.setQueryResultCacheSize(queryResultCacheStats.path("size").asDouble());


		} else {
			
		}

		JsonNode documentCacheStats = cacheNode.path("documentCache").path("stats");

		if (!documentCacheStats.isMissingNode()) {
			this.setDocumentCacheHitRatio(SolrHelper.multipyBy(documentCacheStats.path("hitratio").asDouble(), PERCENT_MULTIPLIER));
			this.setDocumentCacheHitRatioCumulative(SolrHelper.multipyBy(documentCacheStats.path("cumulative_hitratio").asDouble(), PERCENT_MULTIPLIER));
			this.setDocumentCacheSize(documentCacheStats.path("size").asDouble());
		} else {
			//LOG.error("documentCache is disabled in solrconfig.xml");
		}

		JsonNode fieldValueCacheStats = cacheNode.path("fieldValueCache").path("stats");

		if (!fieldValueCacheStats.isMissingNode()) {
			this.setFieldValueCacheHitRatio(SolrHelper.multipyBy(fieldValueCacheStats.path("hitratio").asDouble(), PERCENT_MULTIPLIER));
			this.setFieldValueCacheHitRatioCumulative(SolrHelper.multipyBy(fieldValueCacheStats.path("cumulative_hitratio").asDouble(), PERCENT_MULTIPLIER));
			this.setFieldValueCacheSize(fieldValueCacheStats.path("size").asDouble());
		} else {
			//LOG.error("fieldValueCache is disabled in solrconfig.xml");
		}

		JsonNode filterCacheStats = cacheNode.path("filterCache").path("stats");

		if (!filterCacheStats.isMissingNode()) {
			this.setFilterCacheHitRatio(SolrHelper.multipyBy(filterCacheStats.path("hitratio").asDouble(), PERCENT_MULTIPLIER));
			this.setFilterCacheHitRatioCumulative(SolrHelper.multipyBy(filterCacheStats.path("cumulative_hitratio").asDouble(), PERCENT_MULTIPLIER));
			this.setFilterCacheSize(filterCacheStats.path("size").asDouble());
		} else {
			//LOG.error("filterCache is disabled in solrconfig.xml");
		}
	}

	public Double getQueryResultCacheHitRatio() {
		return queryResultCacheHitRatio;
	}

	public void setQueryResultCacheHitRatio(Double queryResultCacheHitRatio) {
		this.queryResultCacheHitRatio = queryResultCacheHitRatio;
	}

	public Double getDocumentCacheHitRatio() {
		return documentCacheHitRatio;
	}

	public void setDocumentCacheHitRatio(Double documentCacheHitRatio) {
		this.documentCacheHitRatio = documentCacheHitRatio;
	}

	public Double getFieldValueCacheHitRatio() {
		return fieldValueCacheHitRatio;
	}

	public void setFieldValueCacheHitRatio(Double fieldValueCacheHitRatio) {
		this.fieldValueCacheHitRatio = fieldValueCacheHitRatio;
	}

	public Double getFilterCacheHitRatio() {
		return filterCacheHitRatio;
	}

	public void setFilterCacheHitRatio(Double filterCacheHitRatio) {
		this.filterCacheHitRatio = filterCacheHitRatio;
	}

	public Double getQueryResultCacheHitRatioCumulative() {
		return queryResultCacheHitRatioCumulative;
	}

	public void setQueryResultCacheHitRatioCumulative(Double queryResultCacheHitRatioCumulative) {
		this.queryResultCacheHitRatioCumulative = queryResultCacheHitRatioCumulative;
	}

	public Double getDocumentCacheHitRatioCumulative() {
		return documentCacheHitRatioCumulative;
	}

	public void setDocumentCacheHitRatioCumulative(Double documentCacheHitRatioCumulative) {
		this.documentCacheHitRatioCumulative = documentCacheHitRatioCumulative;
	}

	public Double getFieldValueCacheHitRatioCumulative() {
		return fieldValueCacheHitRatioCumulative;
	}

	public void setFieldValueCacheHitRatioCumulative(Double fieldValueCacheHitRatioCumulative) {
		this.fieldValueCacheHitRatioCumulative = fieldValueCacheHitRatioCumulative;
	}

	public Double getFilterCacheHitRatioCumulative() {
		return filterCacheHitRatioCumulative;
	}

	public void setFilterCacheHitRatioCumulative(Double filterCacheHitRatioCumulative) {
		this.filterCacheHitRatioCumulative = filterCacheHitRatioCumulative;
	}

	public Double getQueryResultCacheSize() {
		return queryResultCacheSize;
	}

	public void setQueryResultCacheSize(Double queryResultCacheSize) {
		this.queryResultCacheSize = queryResultCacheSize;
	}

	public Double getDocumentCacheSize() {
		return documentCacheSize;
	}

	public void setDocumentCacheSize(Double documentCacheSize) {
		this.documentCacheSize = documentCacheSize;
	}

	public Double getFieldValueCacheSize() {
		return fieldValueCacheSize;
	}

	public void setFieldValueCacheSize(Double fieldValueCacheSize) {
		this.fieldValueCacheSize = fieldValueCacheSize;
	}

	public Double getFilterCacheSize() {
		return filterCacheSize;
	}

	public void setFilterCacheSize(Double filterCacheSize) {
		this.filterCacheSize = filterCacheSize;
	}
}
