package com.google.sps.servlets;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.maps.errors.ApiException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.sps.agents.WorkoutAgent;
import com.google.sps.data.DialogFlowClient;
import com.google.sps.data.WorkoutPlan;
import com.google.sps.data.YouTubeVideo;
import com.google.sps.utils.VideoUtils;
import com.google.sps.utils.WorkoutProfileUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkoutTestHelper {
  private static Logger log = LoggerFactory.getLogger(WorkoutTestHelper.class);
  @Mock DialogFlowClient dialogFlowMock;
  @Mock UserService userServiceMock;
  @Mock VideoUtils videoUtilsMock;
  @Mock WorkoutProfileUtils workoutProfileUtilsMock;

  @InjectMocks SaveVideoServlet saveVideoServlet;
  @InjectMocks SaveWorkoutsServlet saveWorkoutsServlet;

  HttpServletRequest request;
  HttpServletResponse response;
  WorkoutAgent workoutAgent;
  DatastoreService customDatastore;
  TestHelper testHelper = new TestHelper();

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  /** Default constructor for WorkoutTestHelper to set up testing environment and mocks. */
  public WorkoutTestHelper() {
    helper.setUp();
    customDatastore = DatastoreServiceFactory.getDatastoreService();
    userServiceMock = mock(UserService.class);
    videoUtilsMock = mock(VideoUtils.class);
    workoutProfileUtilsMock = mock(WorkoutProfileUtils.class);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    saveVideoServlet = new TestableSaveVideoServlet();
    saveWorkoutsServlet = new TestableSaveWorkoutsServlet();
    setLoggedIn();
  }

  /**
   * Gets the ArrayList of saved YouTubeVideos from datastore
   *
   * @return ArrayList of JSONObjects of saved YouTubeVideos identical to that which is passed back
   *     to JavaScript
   */
  public ArrayList<JSONObject> getSavedVideos() throws IOException {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);

    saveVideoServlet.doGet(request, response);

    writer.flush();

    ArrayList<JSONObject> videoJsons = new ArrayList<>();
    String str = stringWriter.toString();
    str = str.substring(1, str.length() - 1);
    String[] videoArray = StringUtils.substringsBetween(str, "{", "}");

    for (String videoString : videoArray) {
      // Construct JSONObject
      String jsonString = "{" + videoString + "}";
      JSONObject json = new JSONObject(jsonString);
      videoJsons.add(json);
    }

    return videoJsons;
  }

  /** Saves YouTubeVideo into datastore */
  public void saveVideo() throws IOException {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);

    saveVideoServlet.doPost(request, response);

    verify(request, atLeast(1)).getParameter("workout-video");
    writer.flush();
  }

  /**
   * Returns workout video to be saved when testing
   *
   * @param workoutVideo String containing workout video information
   */
  public void setSavedVideo(String workoutVideo) {
    when(request.getParameter("workout-video")).thenReturn(workoutVideo);
  }

  /**
   * Gets the saved WorkoutPlan from datastore
   *
   * @return ArrayList of JSONObjects of saved WorkoutPlans identical to that which is passed back
   *     to JavaScript
   */
  public ArrayList<JSONObject> getSavedWorkoutPlans() throws IOException {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);

    saveWorkoutsServlet.doGet(request, response);

    writer.flush();

    ArrayList<JSONObject> workoutPlanJsons = new ArrayList<>();
    String str = stringWriter.toString();

    // Cleaning up string to make JSONObjects
    str = str.substring(1, str.length() - 1);
    String toRemove = StringUtils.substringBetween(str, "[[", "]]");
    str = StringUtils.remove(str, toRemove);

    String[] workoutPlanArray = StringUtils.substringsBetween(str, "{", "}");

    for (String workoutPlanString : workoutPlanArray) {
      // Construct JSONObject
      String jsonString = "{" + workoutPlanString + "}";
      JSONObject json = new JSONObject(jsonString);
      workoutPlanJsons.add(json);
    }

    return workoutPlanJsons;
  }

  /** Saves WorkoutPlan into datastore */
  public void saveWorkoutPlan() throws IOException {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);

    saveWorkoutsServlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("workout-plan");
    writer.flush();
  }

  /**
   * Returns WorkoutPlan to be saved when testing
   *
   * @param workoutPlan String containing workout plan information
   */
  public void setSavedWorkoutPlan(String workoutPlan) {
    when(request.getParameter("workout-plan")).thenReturn(workoutPlan);
  }

  /**
   * Sets the parameters for WorkoutAgent instance
   *
   * @param intentName String of expected intent identified by Dialogflow
   * @param parameters json String with expected parameters identified by Dialogflow
   */
  public void setParameters(String intentName, String parameters)
      throws InvalidProtocolBufferException, IOException, ApiException, InterruptedException {

    Map<String, Value> parametersMap = BookAgentServlet.stringToMap(parameters);

    workoutAgent =
        new WorkoutAgent(
            intentName,
            parametersMap,
            userServiceMock,
            customDatastore,
            videoUtilsMock,
            workoutProfileUtilsMock);
  }

  public String getFulfillment() {
    return workoutAgent.getOutput();
  }

  public String getDisplay() {
    return workoutAgent.getDisplay();
  }

  public String getRedirect() {
    return workoutAgent.getRedirect();
  }

  /**
   * Sets the list of videos returned from the VideoUtils mock when a user searches for workout
   * videos
   *
   * @param videos ArrayList of YouTubeVideo objects
   */
  public void setVideoList(ArrayList<YouTubeVideo> videos) throws IOException {
    when(videoUtilsMock.getVideoList(
            any(UserService.class),
            any(String.class),
            any(String.class),
            any(String.class),
            anyInt(),
            any(String.class)))
        .thenReturn(videos);
  }

  /**
   * Sets the WorkoutPlan returned from the VideoUtils mock when a user requests workout plan
   *
   * @param workoutPlan WorkoutPlan to store
   */
  public void setWorkoutPlan(WorkoutPlan workoutPlan) throws IOException {
    when(videoUtilsMock.getWorkoutPlan(
            any(UserService.class),
            any(DatastoreService.class),
            anyInt(),
            anyInt(),
            any(String.class),
            any(String.class)))
        .thenReturn(workoutPlan);
  }

  /**
   * Sets the mocks returned for a default logged-in user. Default user has: email:
   * "test@example.com" user id: "user1"
   */
  public void setLoggedIn() {
    setUser("test@example.com", "user1");
  }

  /** Sets the user service mock to return a logged-out user. */
  public void setLoggedOut() {
    when(userServiceMock.isUserLoggedIn()).thenReturn(false);
  }

  /**
   * Creates a customized user.
   *
   * @param email String containing the user's email.
   * @param id String containing the user's id number.
   */
  public void setUser(String email, String id) {
    when(userServiceMock.isUserLoggedIn()).thenReturn(true);
    when(userServiceMock.getCurrentUser()).thenReturn(new User(email, "authDomain", id));
  }

  /**
   * Retrieves a list of entity objects from the given query for testing purposes to ensure that
   * result in datastore match the expected.
   *
   * @param category String containing the type of entity we are querying for
   * @param userID String representing the user ID number for getting specific info about other
   *     users
   * @return A list of entity objects returned by datastore.
   */
  public List<Entity> fetchDatastoreEntities(String category, String userId) {
    Filter savedWorkoutVideoFilter = new FilterPredicate("userId", FilterOperator.EQUAL, userId);
    Query query = new Query(category).setFilter(savedWorkoutVideoFilter);
    PreparedQuery results = customDatastore.prepare(query);
    return results.asList(FetchOptions.Builder.withDefaults());
  }

  /** Tear down the environment in which tests that use local services can execute */
  public void tearDown() {
    helper.tearDown();
  }

  private class TestableSaveVideoServlet extends SaveVideoServlet {

    @Override
    public UserService createUserService() {
      return userServiceMock;
    }

    @Override
    public DatastoreService createDatastore() {
      return customDatastore;
    }
  }

  private class TestableSaveWorkoutsServlet extends SaveWorkoutsServlet {

    @Override
    public UserService createUserService() {
      return userServiceMock;
    }

    @Override
    public DatastoreService createDatastore() {
      return customDatastore;
    }
  }
}
