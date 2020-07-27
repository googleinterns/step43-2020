package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.users.UserService;
import com.google.protobuf.Value;
import com.google.sps.utils.BooksAgentHelper;
import java.io.IOException;
import java.util.Map;

/**
 * Books Agent handles user's requests for books from Google Books API. It determines appropriate
 * outputs and display information to send to the user interface based on Dialogflow's detected Book
 * intent.
 */
public class BooksAgent implements Agent {
  private final String intentName;
  private BooksAgentHelper helper;

  /**
   * BooksAgent constructor without queryID sets queryID property to the most recent queryID for the
   * specified sessionID
   *
   * @param intentName String containing the specific intent within memory agent that user is
   *     requesting.
   * @param userInput String containing user's request input
   * @param parameters Map containing the detected entities in the user's intent.
   * @param sessionID String containing the unique sessionID for user's session
   * @param userService UserService instance to access userID and other user info.
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public BooksAgent(
      String intentName,
      String userInput,
      Map<String, Value> parameters,
      String sessionID,
      UserService userService,
      DatastoreService datastore)
      throws IOException, IllegalArgumentException {
    this(intentName, userInput, parameters, sessionID, userService, datastore, null);
  }

  /**
   * BooksAgent constructor with queryID
   *
   * @param intentName String containing the specific intent within memory agent that user is
   *     requesting.
   * @param userInput String containing user's request input
   * @param parameters Map containing the detected entities in the user's intent.
   * @param sessionID String containing the unique sessionID for user's session
   * @param userService UserService instance to access userID and other user info.
   * @param datastore DatastoreService instance used to access book info grom database.
   * @param queryID String containing the unique ID for the BookQuery the user is requesting, If
   *     request comes from Book Display interface, then queryID is retrieved from Book Display
   *     Otherwise, queryID is set to the most recent query that the user (sessionID) made.
   */
  public BooksAgent(
      String intentName,
      String userInput,
      Map<String, Value> parameters,
      String sessionID,
      UserService userService,
      DatastoreService datastore,
      String queryID)
      throws IOException, IllegalArgumentException {
    helper =
        new BooksAgentHelper(
            intentName, userInput, parameters, sessionID, userService, datastore, queryID);
    this.intentName = intentName;
    setParameters(parameters);
  }

  @Override
  public void setParameters(Map<String, Value> parameters)
      throws IOException, IllegalArgumentException {
    // Intents that do not require user to be authenticated
    if (intentName.equals("search")) {
      helper.handleNewQueryIntents(intentName, parameters);
    } else if (intentName.equals("more")) {
      helper.handleMoreIntent();
    } else if (intentName.equals("previous")) {
      helper.handlePreviousIntent();
    } else if (intentName.equals("description") || intentName.equals("preview")) {
      helper.handleBookInfoIntents(parameters);
    } else if (intentName.equals("results")) {
      helper.handleResultsIntent();
    } else {
      helper.handleAuthorizationIntents(parameters);
    }
  }

  @Override
  public String getOutput() {
    return helper.getOutput();
  }

  @Override
  public String getDisplay() {
    return helper.getDisplay();
  }

  @Override
  public String getRedirect() {
    return helper.getRedirect();
  }
}
