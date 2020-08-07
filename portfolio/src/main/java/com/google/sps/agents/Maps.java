package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.maps.errors.ApiException;
import com.google.protobuf.Value;
import com.google.sps.data.Location;
import com.google.sps.data.Place;
import com.google.sps.utils.LocationUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Maps Agent */
public class Maps implements Agent {

  private static Logger log = LoggerFactory.getLogger(Maps.class);

  private final String intentName;
  private String fulfillment = null;
  private String display = null;
  private String redirect = null;
  private ArrayList<String> locationWords;
  private String locationFormatted;
  private Location location;

  public Maps(String intentName, Map<String, Value> parameters)
      throws IllegalStateException, IOException, ApiException, InterruptedException,
          ArrayIndexOutOfBoundsException {
    this.intentName = intentName;
    setParameters(parameters);
  }

  public void setParameters(Map<String, Value> parameters)
      throws IllegalStateException, IOException, ApiException, InterruptedException,
          ArrayIndexOutOfBoundsException {
    locationFormatted = LocationUtils.getFormattedAddress("location", parameters);
    locationWords = LocationUtils.getLocationParameters("location", parameters);
    if (intentName.contains("search")) {
      mapsSearch(parameters);
    } else if (intentName.contains("find")) {
      mapsFind(parameters);
    }
  }

  @Override
  public String getOutput() {
    return fulfillment;
  }

  @Override
  public String getDisplay() {
    return display;
  }

  @Override
  public String getRedirect() {
    return redirect;
  }

  private void mapsSearch(Map<String, Value> parameters)
      throws IllegalStateException, IOException, ApiException, InterruptedException,
          ArrayIndexOutOfBoundsException {
    location = Location.create(locationFormatted);
    fulfillment = "Here is the map for: " + locationFormatted;
    Place place = new Place(location.getLng(), location.getLat());
    display = place.toString();
  }

  private void mapsFind(Map<String, Value> parameters)
      throws IllegalStateException, IOException, ApiException, InterruptedException,
          ArrayIndexOutOfBoundsException {
    String attraction = parameters.get("place-attraction").getStringValue();
    location = Location.create(locationFormatted);
    Place place;
    String limitDisplay = "";
    if (parameters.get("number").getStringValue().equals("-1")) {
      place = new Place(attraction, location.getLng(), location.getLat());
    } else {
      int limit = (int) parameters.get("number").getNumberValue();
      if (limit > 0 && limit < 20) {
        limitDisplay = String.valueOf(limit) + " ";
        place = new Place(attraction, location.getLng(), location.getLat(), limit);
      } else {
        fulfillment =
            "Invalid input for number of "
                + attraction
                + ". Please try again with a positive integer between 1 and 20.";
        return;
      }
    }
    fulfillment =
        "Here are the top "
            + limitDisplay
            + "results for "
            + attraction
            + " in "
            + locationFormatted
            + ".";
    display = place.toString();
  }
}
