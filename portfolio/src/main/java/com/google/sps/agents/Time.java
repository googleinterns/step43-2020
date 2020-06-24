package com.google.sps.agents;
 
// Imports the Google Cloud client library
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.sps.data.Output;
import com.google.sps.data.Location;
import com.google.sps.agents.Agent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
 
/**
 * Time Agent
 */
public class Time implements Agent {
    private final String intentName;
  	private String location;
    
    public Time(String intentName, Map<String, Value> parameters) {
      this.intentName = intentName;
      setParameters(parameters);
      //System.out.println(parameters);
    }

	@Override 
	public void setParameters(Map<String, Value> parameters) {
        Struct locationStruct = parameters.get("location").getStructValue();
        Map<String, Value> location_fields = locationStruct.getFieldsMap();
        System.out.println(location_fields);

        if (!location_fields.isEmpty()){
            String island = location_fields.get("island").getStringValue();
            String street = location_fields.get("street-address").getStringValue();
            String city = location_fields.get("city").getStringValue();
            String adminArea = location_fields.get("admin-area").getStringValue();
            String country = location_fields.get("country").getStringValue();
            String zipCode = location_fields.get("zip-code").getStringValue();

            ArrayList<String> location_words = new ArrayList<String>();
            if (!island.isEmpty()){this.location = island;}
            else{
                if (!street.isEmpty()){location_words.add(street);}
                if (!city.isEmpty()){location_words.add(city);}
                if (!adminArea.isEmpty()){location_words.add(adminArea);}
                if (!country.isEmpty()){location_words.add(country);}
                if (!zipCode.isEmpty()){location_words.add(zipCode);}
                if (location_words.size() >0){
                this.location = String.join(", ", location_words);
                }
                else{
                    this.location = "";
                }
            }
        }else{
            this.location = "";
        }
        System.out.println(this.location);
	}
	
	@Override
	public String getOutput() {
      String output = "";
      if (intentName.equals("get") || intentName.equals("context:time")){
          output = getTime(this.location);
      }
	  return output;
	}

	@Override
	public String getDisplay() {
		return null;
	}

	@Override
	public String getRedirect() {
		return null;
    }

    public String getTime(String locationName){
        String ret = "";
        if (locationName.isEmpty()){
            return "Where?";
        }
        else {
          try{
            Location place = new Location(locationName);
            TimeZone timeZone = place.getTimeZone();

            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa");
            sdf.setTimeZone(timeZone);
            //Date date = Calendar.getInstance(timeZone).getTime();
            String currentTime = sdf.format(date);
            ret = "It is " + currentTime + " in " + location;

          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        return ret;
    }
}