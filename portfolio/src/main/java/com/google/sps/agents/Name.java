package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.protobuf.Value;
import com.google.sps.utils.UserUtils;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Name Agent */
public class Name implements Agent {

  private static Logger log = LoggerFactory.getLogger(Name.class);

  private String intentName;
  private String outputText;
  private String userID;
  private String userDisplayName;
  private DatastoreService datastore = createDatastore();
  private UserService userService = createUserService();

  public Name(String intentName, Map<String, Value> parameters) {
    if (intentName == null) { // Required for my mock tests to run
      return;
    }
    this.intentName = intentName;
    if (!userService.isUserLoggedIn()) {
      outputText = "Please login to modify your name.";
    } else {
      setParameters(parameters);
    }
  }

  @Override
  public void setParameters(Map<String, Value> parameters) {
    String nameType = parameters.get("type").getStringValue();
    String name = null;
    nameType = nameType.equals("") ? "first name" : nameType;
    name = getSpecificName(parameters, nameType);
    if (name.equals("")) {
      outputText = "I'm sorry, I didn't catch the name. Can you repeat that?";
    } else {
      UserUtils.saveName(userService, datastore, nameType, name);
      outputText = "Changing your " + nameType + " to be " + name + ".";
      userDisplayName = UserUtils.getDisplayName(userService, datastore);
    }
  }

  private String getSpecificName(Map<String, Value> parameters, String nameType) {
    String name = parameters.get("given-name").getStringValue();
    if (!name.equals("")) {
      return name;
    }
    if (nameType.equals("last name")) {
      return parameters.get("last-name").getStringValue();
    } else if (nameType.equals("nickname")) {
      return parameters.get("nick-name").getStringValue();
    }
    return "";
  }

  @Override
  public String getOutput() {
    return outputText;
  }

  @Override
  public String getDisplay() {
    return userDisplayName;
  }

  @Override
  public String getRedirect() {
    return null;
  }

  protected UserService createUserService() {
    return UserServiceFactory.getUserService();
  }

  protected DatastoreService createDatastore() {
    return DatastoreServiceFactory.getDatastoreService();
  }
}
