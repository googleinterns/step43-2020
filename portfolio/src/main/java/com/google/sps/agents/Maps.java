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
    private Struct location;
    private Map<String, Value> fields;
    private String businessName;
    private String city;
    private String county;
    private String country;
    private String island;
    private String state;
    private String streetAddress;
    private String zipCode;


    public Maps(String intentName, Map<String, Value> parameters) {
      this.intentName = intentName;
      setParameters(parameters);
    }

	@Override 
	public void setParameters(Map<String, Value> parameters) {
        System.out.println(parameters);
        location = parameters.get("location").getStructValue();
        fields = location.getFieldsMap();
        businessName = fields.get("business-name").getStringValue();
        city = fields.get("city").getStringValue();
        county = fields.get("subadmin-area");
        country = fields.get("country").getStringValue();
        island = fields.get("island").getStringValue();
        streetAddress = fields.get("street-address").getStringValue();
        state = fields.get("admin-area").getStringValue();
        zipCode = fields.get("zip-code").getStringValue();
        
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