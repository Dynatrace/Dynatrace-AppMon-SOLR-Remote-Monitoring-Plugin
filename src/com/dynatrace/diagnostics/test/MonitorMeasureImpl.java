 /**
  * This class is used to test the monitor from the command line.
  **/ 
package com.dynatrace.diagnostics.test;

import com.dynatrace.diagnostics.pdk.MonitorMeasure;

public class MonitorMeasureImpl implements MonitorMeasure
{

    private double value;
    private String parameter;
    private String measureName;
    private String metricName;
    private String metricGroupName;

    public void setValue(double value) {
		this.value = value;
	}
	
	//This method is a 6.3 addition!
	public void resetValue() {}

    public String getParameter(String parameter) {
		return parameter;
	}

    public String getMeasureName() {
		return measureName;
	}

    public String getMetricName() {
		return metricName;
	}

    public String getMetricGroupName() {
		return metricGroupName;
	}

	// Not part of the "MonitorMeasure" interface...
	
    public double getValue() {
		return value;
	}

    public void setParameter(String parameter) {
		this.parameter = parameter;
	}

    public void setMeasureName(String measureName) {
		this.measureName = measureName;
	}

    public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

    public void setMetricGroupName(String metricGroupName) {
		this.metricGroupName = metricGroupName;
	}

    public String toString() {
		return String.valueOf(this.getValue());
	}
}
