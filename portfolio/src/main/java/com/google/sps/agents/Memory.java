package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.log.InvalidRequestException;
import com.google.appengine.api.users.UserService;
import com.google.protobuf.Value;
import com.google.sps.data.ConversationOutput;
import com.google.sps.data.Pair;
import com.google.sps.utils.MemoryUtils;
import com.google.sps.utils.TimeUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memory Agent Handles storing user info from past conversation history to user lists and providing
 * the user with this stored information on request. Only works if the user is logged in.
 * Conversation history is automatically updated every time the user converses with the assistant
 * and storing/updating lists and notes occurs on user voice command. Finally, with sufficient user
 * data history, provides recommendations for additional list items to add to list.
 */
public class Memory implements Agent {

  private final String intentName;
  private String userID;
  private String fulfillment;
  private String display;
  private DatastoreService datastore;
  private UserService userService;
  private String listName;
  private ArrayList<String> items = new ArrayList<>();
  private static Logger log = LoggerFactory.getLogger(Memory.class);

  /**
   * Memory agent constructor that uses intent and parameter to determnine fulfillment for user
   * request.
   *
   * @param intentName String containing the specific intent within memory agent that user is
   *     requesting.
   * @param parameters Map containing the detected entities in the user's intent.
   * @param userService UserService instance to access userID and other user info.
   * @param datastore DatastoreService instance used to access past comments from the user's
   *     database.
   */
  public Memory(
      String intentName,
      Map<String, Value> parameters,
      UserService userService,
      DatastoreService datastore)
      throws InvalidRequestException, EntityNotFoundException {
    this.intentName = intentName;
    this.userService = userService;
    this.datastore = datastore;
    setParameters(parameters);
  }

  @Override
  public void setParameters(Map<String, Value> parameters)
      throws InvalidRequestException, EntityNotFoundException {
    if (!userService.isUserLoggedIn()) {
      fulfillment = "Please login to access user history.";
      return;
    }
    userID = userService.getCurrentUser().getUserId();
    if (intentName.contains("keyword")) {
      findKeyword(parameters);
    } else if (intentName.contains("time")) {
      findTimePeriodComments(parameters);
    } else if (intentName.contains("list")) {
      listName = parameters.get("list-name").getStringValue();
      if (listName.isEmpty()) {
        fulfillment = "What would you like to name the list?";
        return;
      }
      unpackObjects(parameters);
      String[] subIntents = intentName.split("-");
      String subListIntent = subIntents[subIntents.length - 1];
      if (subListIntent.contains("make")) {
        makeList(parameters);
      } else if (subListIntent.contains("show")) {
        showList(parameters);
      } else if (subListIntent.contains("custom") || subListIntent.contains("add")) {
        updateList(parameters);
      } else if (subListIntent.contains("yes")) {
        makeMoreRecommendations();
      }
    }
  }

  /**
   * Handles request for conversation history search for a key word.
   *
   * @param parameters Map containing the detected entities in the user's intent.
   */
  private void findKeyword(Map<String, Value> parameters) throws InvalidRequestException {
    String word = parameters.get("keyword").getStringValue();
    List<Pair<Entity, List<Entity>>> conversationList;
    String timePeriodDisplay = "";
    try {
      Value dateObject = parameters.get("date-time-enhanced");
      if (dateObject != null && dateObject.hasStructValue()) {
        Pair<Long, Long> timeRange = getTimeRange(dateObject);
        conversationList =
            MemoryUtils.getKeywordCommentEntitiesWithTime(
                datastore, userID, word.toLowerCase(), timeRange.getKey(), timeRange.getValue());
        timePeriodDisplay = " from " + parameters.get("date-time-original").getStringValue();
      } else {
        conversationList =
            MemoryUtils.getKeywordCommentEntities(datastore, userID, word.toLowerCase());
      }
      if (conversationList.isEmpty()) {
        fulfillment = "Sorry, there were no results matching the keyword \"" + word + ".\"";
      } else {
        fulfillment =
            "Here are all the results"
                + timePeriodDisplay
                + " including the keyword \""
                + word
                + ".\"";
        ConversationOutput convoOutput = new ConversationOutput(word, conversationList);
        display = convoOutput.toString();
      }
    } catch (ParseException e) {
      log.error("Parse error in date-time parameter", e);
      throw new InvalidRequestException("Parse error in date-time parameter");
    }
  }

  /**
   * Handles request for conversation history search for a duration.
   *
   * @param parameters Map containing the detected entities in the user's intent.
   */
  private void findTimePeriodComments(Map<String, Value> parameters)
      throws InvalidRequestException {
    try {
      Pair<Long, Long> timeRange = getTimeRange(parameters.get("date-time-enhanced"));
      List<Entity> conversationSnippet =
          MemoryUtils.getTimePeriodCommentEntities(
              datastore, userID, timeRange.getKey(), timeRange.getValue());
      if (conversationSnippet.isEmpty()) {
        fulfillment =
            "Could not find any conversation from "
                + parameters.get("date-time-original").getStringValue()
                + ".";
      } else {
        fulfillment =
            "Here are all the results from "
                + parameters.get("date-time-original").getStringValue()
                + ".";
        ConversationOutput convoOutput = new ConversationOutput(conversationSnippet);
        display = convoOutput.toString();
      }
    } catch (ParseException e) {
      log.error("Parse error in date-time parameter", e);
      throw new InvalidRequestException("Parse error in date-time parameter");
    }
  }

  /**
   * Retrieves the time range for a duration request.
   *
   * @param datObject Value corresponding to the parameter's 'date-time-enhanced' attribute which
   *     contains date/time info
   * @return A pair where the key is the start time and value is the end time.
   */
  private Pair<Long, Long> getTimeRange(Value dateObject) throws ParseException {
    String startDateString;
    String endDateString;
    Value dateTimeObject = dateObject.getStructValue().getFieldsMap().get("date-time");
    if (dateTimeObject.hasStructValue()) {
      Map<String, Value> durationMap = dateTimeObject.getStructValue().getFieldsMap();
      if (durationMap.get("date-time") != null) {
        // Case where user specifies a specific date and time (should return a 10 min period
        // centered around the time)
        Date dateTime = TimeUtils.stringToDate(durationMap.get("date-time").getStringValue());
        return new Pair(dateTime.getTime() - 300000, dateTime.getTime() + 300000);
      }
      // Case where user specifies a time duration.
      startDateString = durationMap.get("startDate").getStringValue();
      endDateString = durationMap.get("endDate").getStringValue();
    } else {
      // Case where user asks for a date but no time (should return a full day period)
      String dateString = dateTimeObject.getStringValue();
      startDateString = dateString.replaceAll("T([0-9]{2}:){2}[0-9]{2}", "T00:00:00");
      endDateString = dateString.replaceAll("T([0-9]{2}:){2}[0-9]{2}", "T23:59:59");
    }
    Date start = TimeUtils.stringToDate(startDateString);
    Date end = TimeUtils.stringToDate(endDateString);
    return new Pair(start.getTime(), end.getTime());
  }

  /**
   * Creates a new list that is stored in datastore.
   *
   * @param parameters Map containing the detected entities in the user's intent.
   */
  private void makeList(Map<String, Value> parameters) throws EntityNotFoundException {
    if (items.isEmpty()) {
      fulfillment = MemoryUtils.makePastRecommendations(userID, datastore, listName);
      MemoryUtils.allocateList(listName, userID, datastore, items);
      MemoryUtils.saveAggregateListData(datastore, userID, listName, items, true);
      return;
    }
    MemoryUtils.allocateList(listName, userID, datastore, items);
    MemoryUtils.saveAggregateListData(datastore, userID, listName, items, true);
    fulfillment = "Created!";
    makeMoreRecommendations();
  }

  private void showList(Map<String, Value> parameters) {
    // TODO
    fulfillment = "no display yet sorry";
  }

  /**
   * Updates an existing list with new items. If list doesn't exist, creates a brand new list in
   * datastore.
   *
   * @param parameters Map containing the detected entities in the user's intent.
   */
  private void updateList(Map<String, Value> parameters) throws EntityNotFoundException {
    boolean listExists = MemoryUtils.addToList(listName, userID, datastore, items);
    if (!listExists) {
      fulfillment =
          "Your "
              + listName
              + " list has not been created yet, so a new list was created with those items.";
      return;
    }
    fulfillment = "Updated!";
    makeMoreRecommendations();
  }

  /**
   * Makes recommendations for items to add to a list when a list is partially populated.
   * Recommendations are made by finding expected item interest among other users with the same list
   * type and recommending those that align most closely with the current user based on other user's
   * trends.
   */
  private void makeMoreRecommendations() {
    try {
      String suggestedItems = MemoryUtils.makeUserRecommendations(userID, datastore, listName);
      fulfillment +=
          " Based on your list item history, you might be interested in adding "
              + suggestedItems
              + " to your "
              + listName
              + " list.";
    } catch (IllegalStateException | EntityNotFoundException e) {
      log.error("User recommendation error", e);
    }
  }

  /**
   * Converts a string of list items into a list of strings representing each item.
   *
   * @param parameters Map containing the detected entities in the user's intent.
   */
  private void unpackObjects(Map<String, Value> parameters) {
    String listObjects = parameters.get("list-objects").getStringValue();
    if (listObjects.isEmpty()) {
      return;
    }
    String[] commaSplit = listObjects.split(",[\\s]*[and]*[\\s]+");
    if (commaSplit.length > 0) {
      String[] finalSplit = commaSplit[commaSplit.length - 1].split("[\\s]+and[\\s]+");
      items = new ArrayList<>(Arrays.asList(commaSplit));
      items.remove(commaSplit.length - 1);
      items.addAll(new ArrayList<String>(Arrays.asList(finalSplit)));
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
    return null;
  }
}
