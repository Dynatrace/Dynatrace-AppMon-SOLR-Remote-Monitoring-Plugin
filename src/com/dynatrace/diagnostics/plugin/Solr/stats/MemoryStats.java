package com.dynatrace.diagnostics.plugin.solr.stats;

import java.io.IOException;
import java.io.InputStream;

import com.dynatrace.diagnostics.plugin.solr.SolrHelper;
import com.fasterxml.jackson.databind.JsonNode;

public class MemoryStats {
	private Double jvmMemoryUsed;
	private Double jvmMemoryFree;
	private Double jvmMemoryTotal;
	private Double freePhysicalMemorySize;
	private Double totalPhysicalMemorySize;
	private Double committedVirtualMemorySize;
	private Double freeSwapSpaceSize;
	private Double totalSwapSpaceSize;
	private Double openFileDescriptorCount;
	private Double maxFileDescriptorCount;

	public void populateStats(InputStream inputStream) throws IOException {
		JsonNode jsonNode = SolrHelper.getJsonNode(inputStream);
		if (jsonNode != null) {
			JsonNode jvmMBeansNode = jsonNode.path("jvm").path("memory").path("raw");
			JsonNode memoryMBeansNode = jsonNode.path("system");
			if (!jvmMBeansNode.isMissingNode()) {
				this.setJvmMemoryUsed(jvmMBeansNode.path("used").asDouble());
				this.setJvmMemoryFree(jvmMBeansNode.path("free").asDouble());
				this.setJvmMemoryTotal(jvmMBeansNode.path("total").asDouble());

			} else {
				//LOG.error("Missing json node while retrieving jvm memory stats");
			}

			if (!memoryMBeansNode.isMissingNode()) {
				this.setFreePhysicalMemorySize(memoryMBeansNode.path("freePhysicalMemorySize").asDouble());
				this.setTotalPhysicalMemorySize(memoryMBeansNode.path("totalPhysicalMemorySize").asDouble());
				this.setCommittedVirtualMemorySize(memoryMBeansNode.path("committedVirtualMemorySize").asDouble());
				this.setFreeSwapSpaceSize(memoryMBeansNode.path("freeSwapSpaceSize").asDouble());
				this.setTotalSwapSpaceSize(memoryMBeansNode.path("totalSwapSpaceSize").asDouble());
				this.setOpenFileDescriptorCount(memoryMBeansNode.path("openFileDescriptorCount").asDouble());
				this.setMaxFileDescriptorCount(memoryMBeansNode.path("maxFileDescriptorCount").asDouble());
			} else {
				//LOG.error("Missing json node while retrieving system memory stats");
			}
		}
	}

	public Double getJvmMemoryUsed() {
		return jvmMemoryUsed;
	}

	public void setJvmMemoryUsed(Double jvmMemoryUsed) {
		this.jvmMemoryUsed = jvmMemoryUsed;
	}

	public Double getJvmMemoryFree() {
		return jvmMemoryFree;
	}

	public void setJvmMemoryFree(Double jvmMemoryFree) {
		this.jvmMemoryFree = jvmMemoryFree;
	}

	public Double getJvmMemoryTotal() {
		return jvmMemoryTotal;
	}

	public void setJvmMemoryTotal(Double jvmMemoryTotal) {
		this.jvmMemoryTotal = jvmMemoryTotal;
	}

	public Double getFreePhysicalMemorySize() {
		return freePhysicalMemorySize;
	}

	public void setFreePhysicalMemorySize(Double freePhysicalMemorySize) {
		this.freePhysicalMemorySize =(freePhysicalMemorySize);
	}

	public Double getTotalPhysicalMemorySize() {
		return totalPhysicalMemorySize;
	}

	public void setTotalPhysicalMemorySize(Double totalPhysicalMemorySize) {
		this.totalPhysicalMemorySize = (totalPhysicalMemorySize);
	}

	public Double getCommittedVirtualMemorySize() {
		return committedVirtualMemorySize;
	}

	public void setCommittedVirtualMemorySize(Double committedVirtualMemorySize) {
		this.committedVirtualMemorySize = (committedVirtualMemorySize);
	}

	public Double getFreeSwapSpaceSize() {
		return freeSwapSpaceSize;
	}

	public void setFreeSwapSpaceSize(Double freeSwapSpaceSize) {
		this.freeSwapSpaceSize = (freeSwapSpaceSize);
	}

	public Double getTotalSwapSpaceSize() {
		return totalSwapSpaceSize;
	}

	public void setTotalSwapSpaceSize(Double totalSwapSpaceSize) {
		this.totalSwapSpaceSize = (totalSwapSpaceSize);
	}

	public Double getOpenFileDescriptorCount() {
		return openFileDescriptorCount;
	}

	public void setOpenFileDescriptorCount(Double openFileDescriptorCount) {
		this.openFileDescriptorCount = openFileDescriptorCount;
	}

	public Double getMaxFileDescriptorCount() {
		return maxFileDescriptorCount;
	}

	public void setMaxFileDescriptorCount(Double maxFileDescriptorCount) {
		this.maxFileDescriptorCount = maxFileDescriptorCount;
	}
}
