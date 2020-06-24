package com.google.sps.agents;
 
// Imports the Google Cloud client library
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.sps.data.Output;
import com.google.sps.agents.Agent;
import java.io.IOException;
import java.util.Map;
 
/**
 * Maps Agent
 */
public class Maps implements Agent {
    private final String intentName;
  	private String currencyFrom;
    private String currencyTo;
    private Double amount;

    public Maps(String intentName, Map<String, Value> parameters) {
      this.intentName = intentName;
      setParameters(parameters);
    }

	@Override 
	public void setParameters(Map<String, Value> parameters) {
	
	}
	
	@Override
	public String getOutput() {
	  return null;
	}

	@Override
	public String getDisplay() {
		return null;
	}

	@Override
	public String getRedirect() {
        return null;
    }
}