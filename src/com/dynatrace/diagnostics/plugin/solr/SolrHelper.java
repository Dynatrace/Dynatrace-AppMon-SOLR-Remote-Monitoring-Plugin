package com.dynatrace.diagnostics.plugin.solr;

import java.io.IOException;
import java.io.InputStream;

import java.text.NumberFormat;
import java.text.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.base.Strings;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SolrHelper {
	private static final double BYTES_CONVERSION_FACTOR = 1024.0;
	CloseableHttpClient httpClient = HttpClients.createDefault();

	private static final Logger log = Logger.getLogger(SolrHelper.class.getName());

	public SolrHelper(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public Map<String, JsonNode> getSolrMBeansHandlersMap(String core, String mbeansUri) throws IOException {
		String uri = String.format(mbeansUri, core);
		InputStream inputStream = null;
		Map<String, JsonNode> solrStatsMap = new HashMap<String, JsonNode>();
		try {
			HttpGet request = new HttpGet(uri);
			HttpResponse response = httpClient.execute(request);
			inputStream = response.getEntity().getContent();
			
			JsonNode jsonNode = getJsonNode(inputStream);
			if (jsonNode != null) {
				JsonNode solrMBeansNode = jsonNode.path("solr-mbeans");
				if (solrMBeansNode.isMissingNode()) {
					throw new IllegalArgumentException("Missing node while parsing solr-mbeans node json string for " + core + uri);
				}
				for (int i = 1; i <= solrMBeansNode.size(); i += 2) {
					solrStatsMap.put(solrMBeansNode.get(i - 1).asText(), solrMBeansNode.get(i));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return solrStatsMap;
	}

	public Map<String, JsonNode> getDataImportStatusMap(String core, String dataImportUri) throws IOException {
		String uri = String.format(dataImportUri, core);
		InputStream inputStream = null;
		Map<String, JsonNode> dataImportStatusMap = new HashMap<String, JsonNode>();
		try {
			HttpGet request = new HttpGet(uri);
			HttpResponse response = httpClient.execute(request);
			inputStream = response.getEntity().getContent();
			
			JsonNode jsonNode = getJsonNode(inputStream);
			if (jsonNode != null) {
				JsonNode statusMessagesNode = jsonNode.path("statusMessages");
				if (statusMessagesNode.isMissingNode()) {
					throw new IllegalArgumentException("Missing node while parsing statusMessages node json string for " + core + uri);
				}
				String name = "Total Requests made to DataSource";
				if (statusMessagesNode.has(name)) {
					dataImportStatusMap.put(name, statusMessagesNode.get(name));
				}
				name = "Total Rows Fetched";
				if (statusMessagesNode.has(name)) {
					dataImportStatusMap.put(name, statusMessagesNode.get(name));
				}
				name = "Total Documents Processed";
				if (statusMessagesNode.has(name)) {
					dataImportStatusMap.put(name, statusMessagesNode.get(name));
				}
				name = "Total Documents Skipped";
				if (statusMessagesNode.has(name)) {
					dataImportStatusMap.put(name, statusMessagesNode.get(name));
				}
				name = "Time taken";
				if (statusMessagesNode.has(name)) {
					dataImportStatusMap.put(name, statusMessagesNode.get(name));
				}
				name = "";
				if (statusMessagesNode.has(name)) {
					dataImportStatusMap.put(name, statusMessagesNode.get(name));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return dataImportStatusMap;
	}

	public List<String> getCores(String uri) {
		List<String> cores = new ArrayList<String>();
		InputStream inputStream = null;
		try {
			HttpGet request = new HttpGet(uri);
			HttpResponse response = httpClient.execute(request);
			inputStream = response.getEntity().getContent();
			JsonNode node = getJsonNode(inputStream);
			if (node != null) {
				Iterator<String> fieldNames = node.path("status").fieldNames();
				
				while (fieldNames.hasNext()) {
					cores.add(fieldNames.next());
				}
				if (cores.isEmpty()) {
					throw new RuntimeException();
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw new RuntimeException();
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return cores;
	}
	
	public String getDefaultCore(String uri) {
		String defaultCore = "";
		InputStream inputStream = null;
		try {
			HttpGet request = new HttpGet(uri);
			HttpResponse response = httpClient.execute(request);
			inputStream = response.getEntity().getContent();
			
			JsonNode node = getJsonNode(inputStream);
			if (node != null) {
				defaultCore = node.path("defaultCoreName").asText();
			}
		} catch (Exception e) {
			throw new RuntimeException();
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return defaultCore;
	}

	public static JsonNode getJsonNode(InputStream inputStream) throws IOException {
		if (inputStream == null) {
			return null;
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(inputStream, JsonNode.class);
	}

	public boolean checkIfMBeanHandlerSupported(String resource) throws IOException {
		InputStream inputStream = null;
		try {
			HttpGet request = new HttpGet(resource);
			HttpResponse response = httpClient.execute(request);
			inputStream = response.getEntity().getContent();
			
			JsonNode jsonNode = getJsonNode(inputStream);
			if (jsonNode != null) {
				JsonNode node = jsonNode.findValue("QUERYHANDLER");
				if (node == null) {
					return false;
				}
				boolean mbeanSupport = node.has("/admin/mbeans");
				if (!mbeanSupport) {
				}
				return mbeanSupport;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception e) {
				// Ignore
			}
		}
	}

	private static Double unLocalizeStrValue(String valueStr) {
		try {
			Locale loc = Locale.getDefault();
			return Double.valueOf(NumberFormat.getInstance(loc).parse(valueStr).doubleValue());
		} catch (ParseException e) {
			// Ignore
		}
		return null;
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	public void setHttpClient(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public static Double multipyBy(Double value, int multiplier) {
		if (value != null) {
			value = value * multiplier;
		}
		return value;
	}
}
