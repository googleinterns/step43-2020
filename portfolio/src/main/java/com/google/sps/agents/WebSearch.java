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
 * WebSearch Agent
 */
public class WebSearch implements Agent {
    private final String intentName;
  	private String searchText;
    
    public WebSearch(String intentName, Map<String, Value> parameters) {
      this.intentName = intentName;
      setParameters(parameters);
    }

	@Override 
	public void setParameters(Map<String, Value> parameters) {
	  searchText = parameters.get("q").getStringValue();
	}
	
	@Override
	public String getOutput() {
	  return "Redirecting to Google Search for " + searchText;
	}

	@Override
	public String getDisplay() {
		return null;
	}

	@Override
	public String getRedirect() {
        String baseURL = "http://www.google.com/search?q=";
		String[] individualWords = searchText.split(" ");
        String endURL = String.join("+", individualWords);
		return baseURL + endURL;
    }
}