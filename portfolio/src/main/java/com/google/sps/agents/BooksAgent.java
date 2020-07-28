package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.users.UserService;
import com.google.protobuf.Value;
import com.google.sps.utils.BookUtils;
import com.google.sps.utils.BooksAgentHelper;
import com.google.sps.utils.OAuthHelper;
import com.google.sps.utils.PeopleUtils;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Books Agent handles user's requests for books from Google Books API. It determines appropriate
 * outputs and display information to send to the user interface based on Dialogflow's detected Book
 * intent.
 */
public class BooksAgent implements Agent {
  private static Logger log = LoggerFactory.getLogger(BooksAgent.class);
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
    this(
        intentName,
        userInput,
        parameters,
        sessionID,
        userService,
        datastore,
        queryID,
        null,
        null,
        null);
  }

  /**
   * BooksAgent constructor for testing purposes, specifying BooksAgentHelper object
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
   * @param oauthHelper OAuthHelper instance used to access OAuth methods
   * @param bookUtils BookUtils instance used to access Google Books API
   * @param peopleUtils PeopleUtils instance used to access Google People API
   */
  public BooksAgent(
      String intentName,
      String userInput,
      Map<String, Value> parameters,
      String sessionID,
      UserService userService,
      DatastoreService datastore,
      String queryID,
      OAuthHelper oauthHelper,
      BookUtils bookUtils,
      PeopleUtils peopleUtils)
      throws IOException, IllegalArgumentException {
    if (oauthHelper == null) {
      oauthHelper = new OAuthHelper();
    }
    if (bookUtils == null) {
      bookUtils = new BookUtils();
    }
    if (peopleUtils == null) {
      peopleUtils = new PeopleUtils();
    }
    this.helper =
        new BooksAgentHelper(
            intentName,
            userInput,
            parameters,
            sessionID,
            userService,
            datastore,
            queryID,
            oauthHelper,
            bookUtils,
            peopleUtils);
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
