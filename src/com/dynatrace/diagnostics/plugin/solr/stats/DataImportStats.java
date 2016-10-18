package com.dynatrace.diagnostics.plugin.solr.stats;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataImportStats {
	private Double totalRequestsMadeToDataSource;
	private Double totalRowsFetched;
	private Double totalDocumentsProcessed;
	private Double totalDocumentsSkipped;
	private Double timeTaken;
	private Double documentsAddedUpdated;
	private Double documentsDeleted;

	private static final Logger log = Logger.getLogger(DataImportStats.class.getName());

	//EXAMPLE: 0:0:1.207
	private static Pattern timeTakenPattern = Pattern.compile("(\\d{1,2}):(\\d{1,2}):(\\d{1,2}).(\\d{3})");

	//EXAMPLE: Indexing completed. Added/Updated: 16 documents. Deleted 0 documents.
	private static Pattern documentsAddedUpdatedPattern = Pattern.compile(".*Added/Updated: (\\d+) .*");
	private static Pattern documentsDeletedPattern = Pattern.compile(".*Deleted (\\d+) documents.*");
	
	public void populateStats(Map<String, JsonNode> dataImportStatusMap) {
		JsonNode node = dataImportStatusMap.get("Total Requests made to DataSource");
		if (node != null) {
			setTotalRequestsMadeToDataSource(node.asDouble());
		}
		node = dataImportStatusMap.get("Total Rows Fetched");
		if (node != null) {
			setTotalRowsFetched(node.asDouble());
		}
		node = dataImportStatusMap.get("Total Documents Processed");
		if (node != null) {
			setTotalDocumentsProcessed(node.asDouble());
		}
		node = dataImportStatusMap.get("Total Documents Skipped");
		if (node != null) {
			setTotalDocumentsSkipped(node.asDouble());
		}
		node = dataImportStatusMap.get("Time taken");
		if (node != null) {
			setTimeTaken(dateParseRegExp(node.asText()));
		}
		//The empty name contains the "Indexing completed" message
		node = dataImportStatusMap.get("");
		if (node != null) {
			setDocumentsAddedUpdated(documentsAddedUpdatedParseRegExp(node.asText()));
			setDocumentsDeleted(documentsDeletedParseRegExp(node.asText()));
		}
	}

	public Double getTotalRequestsMadeToDataSource() {
		return totalRequestsMadeToDataSource;
	}

	public Double getTotalRowsFetched() {
		return totalRowsFetched;
	}

	public Double getTotalDocumentsProcessed() {
		return totalDocumentsProcessed;
	}

	public Double getTotalDocumentsSkipped() {
		return totalDocumentsSkipped;
	}

	public Double getTimeTaken() {
		return timeTaken;
	}

	public Double getDocumentsAddedUpdated() {
		return documentsAddedUpdated;
	}

	public Double getDocumentsDeleted() {
		return documentsDeleted;
	}

	public void setTotalRequestsMadeToDataSource(Double totalRequestsMadeToDataSource) {
		this.totalRequestsMadeToDataSource = totalRequestsMadeToDataSource;
	}

	public void setTotalRowsFetched(Double totalRowsFetched) {
		this.totalRowsFetched = totalRowsFetched;
	}

	public void setTotalDocumentsProcessed(Double totalDocumentsProcessed) {
		this.totalDocumentsProcessed = totalDocumentsProcessed;
	}

	public void setTotalDocumentsSkipped(Double totalDocumentsSkipped) {
		this.totalDocumentsSkipped = totalDocumentsSkipped;
	}

	public void setTimeTaken(Double timeTaken) {
		this.timeTaken = timeTaken;
	}

	public void setDocumentsAddedUpdated(Double documentsAddedUpdated) {
		this.documentsAddedUpdated = documentsAddedUpdated;
	}

	public void setDocumentsDeleted(Double documentsDeleted) {
		this.documentsDeleted = documentsDeleted;
	}

	private Double dateParseRegExp(String timeTaken) {
		Matcher matcher = timeTakenPattern.matcher(timeTaken);
		if (matcher.matches()) {
			return new Double(matcher.group(1)) * 3600000 
				+ new Double(matcher.group(2)) * 60000 
				+ new Double(matcher.group(3)) * 1000 
				+ new Double(matcher.group(4)); 
		} else {
			throw new IllegalArgumentException("Invalid format " + timeTaken);
		}
	}	

	private Double documentsAddedUpdatedParseRegExp(String documentsAddedUpdated) {
		Matcher matcher = documentsAddedUpdatedPattern.matcher(documentsAddedUpdated);
		if (matcher.matches()) {
			return new Double(matcher.group(1)); 
		} else {
			throw new IllegalArgumentException("Invalid format " + documentsAddedUpdated);
		}
	}	

	private Double documentsDeletedParseRegExp(String documentsDeleted) {
		Matcher matcher = documentsDeletedPattern.matcher(documentsDeleted);
		if (matcher.matches()) {
			return new Double(matcher.group(1)); 
		} else {
			throw new IllegalArgumentException("Invalid format " + documentsDeleted);
		}
	}
}