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
 * Unit Converter Agent
 */
public class UnitConverter implements Agent {
    private final String intentName;

  	private String unitFrom;
    private String unitTo;
    private Double amount;

    public UnitConverter(String intentName, Map<String, Value> parameters) {
      this.intentName = intentName;
      setParameters(parameters);
    }

	@Override 
	public void setParameters(Map<String, Value> parameters) {
    
	  unitFrom = parameters.get("unit-from").getStringValue();
      unitTo = parameters.get("unit-to").getStringValue();
      amount = parameters.get("amount").getNumberValue();
	}
	
	@Override
	public String getOutput() {
	  return "Redirecting for conversion";
	}

	@Override
	public String getDisplay() {
		return null;
	}

	@Override
	public String getRedirect() {

    String baseURL = "http://www.google.com/search?q=";
    String endURL = String.join("+", "Convert", String.valueOf(amount), unitFrom, "to", unitTo);
		return baseURL + endURL;
  }
}