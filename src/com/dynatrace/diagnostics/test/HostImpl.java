 /**
  * This class is used to test the monitor from the command line.
  **/ 
package com.dynatrace.diagnostics.test;

public class HostImpl implements com.dynatrace.diagnostics.pdk.PluginEnvironment.Host {
	
    public java.lang.String getAddress() {return "localhost";}
}

