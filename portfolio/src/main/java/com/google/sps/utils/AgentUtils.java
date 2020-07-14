package com.google.sps.utils;

// Imports the Google Cloud client library
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.users.UserService;
import com.google.cloud.translate.TranslateException;
import com.google.maps.errors.ApiException;
import com.google.protobuf.ByteString;
import com.google.protobuf.Value;
import com.google.sps.agents.*;
import com.google.sps.data.DialogFlowClient;
import com.google.sps.data.Output;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Identifies agent from Dialogflow API Query result and creates Output object */
public class AgentUtils {

  public static String detectedInput;
  private static UserService userService;
  private static DatastoreService datastore;
  private static Logger log = LoggerFactory.getLogger(Name.class);

  /**
   * Method that creates and returns an Output object which is passed to frontend JS that
   * fulfillment and display info for user requests. If no backend fulfillment is necessary for the
   * given intent, it defaults to an Output object containing the standard Dialogflow response. If
   * the intent cannot be understood, it returns an Output object with a default fulfillment stating
   * that intent was not heard.
   *
   * @param queryResult DialogFlowClient object which contains all attributes of Dialogflow's intent
   *     detection.
   * @param languageCode String containing the language to use for the audio file returned in the
   *     Output object.
   * @param userServiceInput UserService instance to access userID and other user info if necessary.
   * @param datastoreInput DatastoreService instance used to access past comments from the user's
   *     database if necessary.
   */
  public static Output getOutput(
      DialogFlowClient queryResult,
      String languageCode,
      UserService userServiceInput,
      DatastoreService datastoreInput) {
    String fulfillment = null;
    String display = null;
    String redirect = null;
    byte[] byteStringToByteArray = null;
    Agent object = null;

    String detectedIntent = queryResult.getIntentName();
    Boolean allParamsPresent = queryResult.getAllRequiredParamsPresent();
    String agentName = getAgentName(detectedIntent);
    String intentName = getIntentName(detectedIntent);
    userService = userServiceInput;
    datastore = datastoreInput;

    // Retrieve detected input from DialogFlow result.
    detectedInput = queryResult.getQueryText();
    if (detectedInput.equals("")) {
      return null;
    }
    Map<String, Value> parameterMap = queryResult.getParameters();

    // Default fulfillment if all required parameters are not present
    fulfillment = queryResult.getFulfillmentText();

    // Set fulfillment if parameters are present, upon any exceptions return default
    if (allParamsPresent) {
      try {
        object = createAgent(agentName, intentName, detectedInput, parameterMap);
        fulfillment = object.getOutput();
        fulfillment = fulfillment == null ? queryResult.getFulfillmentText() : fulfillment;
        display = object.getDisplay();
        redirect = object.getRedirect();
      } catch (IllegalStateException
          | IOException
          | ApiException
          | InterruptedException
          | IllegalArgumentException
          | ArrayIndexOutOfBoundsException
          | NullPointerException
          | TranslateException e) {
        log.info("Error in object creation.");
        e.printStackTrace();
      }
    }
    if (fulfillment.equals("")) {
      fulfillment = "I'm sorry, I didn't catch that. Can you repeat that?";
    }
    if (userService.isUserLoggedIn()) {
      UserUtils.saveComment(
          userService.getCurrentUser().getUserId(), datastore, detectedInput, fulfillment);
    }
    byteStringToByteArray = getByteStringToByteArray(fulfillment, languageCode);
    Output output =
        new Output(
            detectedInput, fulfillment, byteStringToByteArray, display, redirect, detectedIntent);
    return output;
  }

  private static Agent createAgent(
      String agentName, String intentName, String queryText, Map<String, Value> parameterMap)
      throws IllegalStateException, IOException, ApiException, InterruptedException,
          ArrayIndexOutOfBoundsException {
    switch (agentName) {
      case "books":
        return new BooksAgent(intentName, queryText, parameterMap);
      case "calculator":
        return new Tip(intentName, parameterMap);
      case "currency":
        return new Currency(intentName, parameterMap);
      case "date":
        return new Date(intentName, parameterMap);
      case "language":
        return new Language(intentName, parameterMap);
      case "maps":
        return new Maps(intentName, parameterMap);
      case "memory":
        return new Memory(intentName, parameterMap, userService, datastore);
      case "name":
        return new Name(intentName, parameterMap, userService, datastore);
      case "reminders":
        return new Reminders(intentName, parameterMap);
      case "time":
        return new Time(intentName, parameterMap);
      case "translate":
        return new TranslateAgent(intentName, parameterMap);
      case "units":
        return new UnitConverter(intentName, parameterMap);
      case "weather":
        return new Weather(intentName, parameterMap);
      case "web":
        return new WebSearch(intentName, parameterMap);
      case "workout":
        return new WorkoutAgent(intentName, parameterMap);
      default:
        return null;
    }
  }

  private static String getAgentName(String detectedIntent) {
    String[] intentList = detectedIntent.split("\\.", 2);
    return intentList[0];
  }

  private static String getIntentName(String detectedIntent) {
    String[] intentList = detectedIntent.split("\\.", 2);
    String intentName = detectedIntent;
    if (intentList.length > 1) {
      return intentList[1];
    }
    return intentName;
  }

  public static String getUserInput() {
    return detectedInput;
  }

  public static byte[] getByteStringToByteArray(String fulfillment, String languageCode) {
    byte[] byteArray = null;
    try {
      ByteString audioResponse = SpeechUtils.synthesizeText(fulfillment, languageCode);
      byteArray = audioResponse.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return byteArray;
  }

  public static String getLanguageCode(String language) {
    if (language == null) {
      return "en-US";
    }

    switch (language) {
      case "Chinese":
        return "zh-CN";
      case "English":
        return "en-US";
      case "French":
        return "fr";
      case "German":
        return "de";
      case "Hindi":
        return "hi";
      case "Italian":
        return "it";
      case "Japanese":
        return "ja";
      case "Korean":
        return "ko";
      case "Portuguese":
        return "pt";
      case "Russian":
        return "ru";
      case "Spanish":
        return "es";
      case "Swedish":
        return "sv";
      default:
        return null;
    }
  }
}
