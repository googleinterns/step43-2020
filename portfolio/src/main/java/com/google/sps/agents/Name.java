package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.users.UserService;
import com.google.protobuf.Value;
import com.google.sps.utils.UserUtils;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Name Agent */
public class Name implements Agent {

  private static Logger log = LoggerFactory.getLogger(Name.class);

  private String intentName;
  String outputText;
  String userID;
  String userDisplayName;
  DatastoreService datastore;
  UserService userService;

  /**
   * Name agent constructor that uses intent and parameter to determnine fulfillment for user
   * request.
   *
   * @param intentName String containing the specific intent within memory agent that user is
   *     requesting.
   * @param parameters Map containing the detected entities in the user's intent.
   * @param userService UserService instance to access userID and other user info.
   * @param datastore DatastoreService instance used to access past comments from the user's
   *     database.
   */
  public Name(
      String intentName,
      Map<String, Value> parameters,
      UserService userService,
      DatastoreService datastore) {
    this.intentName = intentName;
    this.datastore = datastore;
    this.userService = userService;
    if (!userService.isUserLoggedIn()) {
      outputText = "Please login to modify your name.";
    } else {
      userID = userService.getCurrentUser().getUserId();
      setParameters(parameters);
    }
  }

  @Override
  public void setParameters(Map<String, Value> parameters) {
    log.info("parameters: " + parameters);
    String nameType = parameters.get("type").getStringValue();
    String name = null;
    nameType = nameType.equals("") ? "first name" : nameType;
    name = getSpecificName(parameters, nameType);
    log.info("name: " + name);
    if (name.equals("")) {
      outputText = "I'm sorry, I didn't catch the name. Can you repeat that?";
    } else {
      UserUtils.saveName(userService.getCurrentUser().getUserId(), datastore, nameType, name);
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
}
