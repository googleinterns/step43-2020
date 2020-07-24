package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.Struct.Builder;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.sps.agents.*;
import com.google.sps.agents.BooksAgent;
import com.google.sps.data.BookQuery;
import com.google.sps.data.Output;
import com.google.sps.utils.AgentUtils;
import com.google.sps.utils.BooksMemoryUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 * Creates BookAgent object and retrieves corresponding Output object for a given intent. For all
 * intents passed to book-agent servlet, no queryText or parameterMap will be necessary.
 */
@WebServlet("/book-agent")
public class BookAgentServlet extends HttpServlet {

  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private UserService userService = UserServiceFactory.getUserService();
  private String bookshelfName = "";
  /**
   * Retrieves corresponding Output object for given Book intent passed as a parameter to request.
   * If a number parameter was passed to request, then it is placed into parameterMap to be sent to
   * Book Agent.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");

    String intent = request.getParameter("intent");
    String sessionID = request.getParameter("session-id");
    String language = request.getParameter("language");
    String queryID = request.getParameter("query-id");
    String languageCode = AgentUtils.getLanguageCode(language);
    Map<String, Value> parameterMap = null;
    Output output = null;

    try {
      ArrayList<String> params = new ArrayList<String>();
      if (request.getParameter("number") != null) {
        params.add("\"number\": " + request.getParameter("number"));
      }
      if (request.getParameter("bookshelf") != null) {
        this.bookshelfName = request.getParameter("bookshelf");
        params.add("\"bookshelf\": \"" + bookshelfName + "\"");
      }
      if (!params.isEmpty()) {
        String paramString = String.join(",", params);
        parameterMap = stringToMap("{" + paramString + "}");
      }
      output =
          getOutputFromBookAgent(intent, sessionID, parameterMap, languageCode, queryID, datastore);
    } catch (Exception e) {
      e.printStackTrace();
    }

    String json = new Gson().toJson(output);
    response.getWriter().write(json);
  }

  /**
   * Creates a BookAgent to construct an Output object based on given intent.
   *
   * @param intent intent String passed as parameter to Servlet
   * @param sessionID unique ID for current session
   * @param parameterMap map containing parameters needed, if any, by BookAgent
   * @param languageCode language code
   * @param queryID unique ID (within sessionID) for current query
   * @param datastore DataStore service to use
   * @return Output object to be sent to frontend
   */
  public Output getOutputFromBookAgent(
      String intent,
      String sessionID,
      Map<String, Value> parameterMap,
      String languageCode,
      String queryID,
      DatastoreService datastore) {
    String display = null;
    String redirect = null;
    byte[] byteStringToByteArray = null;
    String intentName = AgentUtils.getIntentName(intent);
    String detectedInput = "Button pressed for: " + intentName;
    String userInput = detectedInput;

    if (intentName.equals("library")) {
      userInput = "Show me my " + bookshelfName + " bookshelf.";
    } else if (intentName.equals("add")) {
      if (bookshelfName.isEmpty()) {
        userInput = "Add to My Library.";
      } else {
        userInput = "Add to my " + bookshelfName + " bookshelf.";
      }
    } else {
      BookQuery query = BooksMemoryUtils.getStoredBookQuery(sessionID, queryID, datastore);
      userInput = query.getUserInput();
    }

    String fulfillment = "";
    try {
      BooksAgent agent =
          new BooksAgent(
              intentName, userInput, parameterMap, sessionID, userService, datastore, queryID);
      fulfillment = agent.getOutput();
      display = agent.getDisplay();
      redirect = agent.getRedirect();
    } catch (IOException
        | IllegalArgumentException
        | ArrayIndexOutOfBoundsException
        | NullPointerException e) {
      e.printStackTrace();
    }
    if (fulfillment.equals("")) {
      fulfillment = "I'm sorry, I didn't catch that. Can you repeat that?";
    }
    byteStringToByteArray = AgentUtils.getByteStringToByteArray(fulfillment, languageCode);
    Output output =
        new Output(userInput, fulfillment, byteStringToByteArray, display, redirect, intent);
    return output;
  }

  /**
   * Converts a json string into a Map object
   *
   * @param json json string
   * @return Map<String, Value>
   */
  public static Map<String, Value> stringToMap(String json) throws InvalidProtocolBufferException {
    JSONObject jsonObject = new JSONObject(json);
    Builder structBuilder = Struct.newBuilder();
    JsonFormat.parser().merge(jsonObject.toString(), structBuilder);
    Struct struct = structBuilder.build();
    return struct.getFieldsMap();
  }

  public static String loadUserInput(String sessionID, String queryID, DatastoreService datastore) {
    BookQuery query = BooksMemoryUtils.getStoredBookQuery(sessionID, queryID, datastore);
    return query.getUserInput();
  }
}
