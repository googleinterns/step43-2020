package com.google.sps.servlets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.maps.errors.ApiException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.sps.data.Output;
import com.google.sps.data.WorkoutPlan;
import com.google.sps.data.YouTubeVideo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.*;
import org.apache.commons.lang3.SerializationUtils;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WorkoutPlanTest {

  private WorkoutTestHelper workoutTester;
  private ArrayList<WorkoutPlan> workoutPlans;
  private ArrayList<WorkoutPlan> savedWorkoutPlans;
  private String parameters;
  private static Logger log = LoggerFactory.getLogger(WorkoutPlanTest.class);

  @Before
  public void setUp() {
    workoutTester = new WorkoutTestHelper();
  }

  @After
  public void tearDown() {
    workoutTester.tearDown();
  }

  /** Checks if WorkoutPlan search results is stored in datastore if user is logged in */
  @Test
  public void testWorkoutPlanStoringWhenUserLoggedIn()
      throws IOException, InvalidProtocolBufferException, ApiException, InterruptedException {

    workoutPlans = getWorkoutPlans();
    parameters =
        "{\"date-time\": { \"startDateTime\": \"2020-08-02T19:00:18-07:00\", \"endDateTime\": \"2020-08-08T19:00:18-07:00\" }, \"workout-type\": \"HIIT\"}";

    // Storing WorkoutPlan when creating agent
    for (WorkoutPlan workoutPlan : workoutPlans) {
      workoutTester.setWorkoutPlan(workoutPlan);
      workoutTester.setParameters("workout.plan", parameters);
    }

    ArrayList<Integer> workoutPlanIdList = getWorkoutPlanIds(workoutPlans);

    // Assertions
    List<Entity> storedEntities = workoutTester.fetchDatastoreEntities("WorkoutPlan", "user1");

    for (Entity workoutPlanEntity : storedEntities) {
      Blob workoutPlanBlob = (Blob) workoutPlanEntity.getProperty("workoutPlan");
      WorkoutPlan workoutPlan = SerializationUtils.deserialize(workoutPlanBlob.getBytes());
      assertTrue(workoutPlanIdList.contains(workoutPlan.getWorkoutPlanId()));
    }

    assertNotNull(workoutTester.getDisplay());
  }

  /** Checks that WorkoutPlan search results are not stored in datastore if user is logged out */
  @Test
  public void testWorkoutPlanStoringWhenUserLoggedOut()
      throws IOException, InvalidProtocolBufferException, ApiException, InterruptedException {
    workoutTester.setLoggedOut();
    workoutPlans = getWorkoutPlans();
    parameters =
        "{\"date-time\": { \"startDateTime\": \"2020-08-02T19:00:18-07:00\", \"endDateTime\": \"2020-08-08T19:00:18-07:00\" }, \"workout-type\": \"HIIT\"}";

    for (WorkoutPlan workoutPlan : workoutPlans) {
      workoutTester.setWorkoutPlan(workoutPlan);
      workoutTester.setParameters("workout.plan", parameters);
    }

    // Assertions
    List<Entity> storedEntities = workoutTester.fetchDatastoreEntities("WorkoutPlan", "user1");
    int numStoredWorkoutPlanEntities = storedEntities.size();
    assertEquals(0, numStoredWorkoutPlanEntities);
    assertNotNull(workoutTester.getDisplay());
  }

  /**
   * Checks if WorkoutPlans specifically saved by user are stored in datastore if user is logged in
   */
  @Test
  public void testWorkoutPlanSavingWhenUserLoggedIn()
      throws IOException, InvalidProtocolBufferException, ApiException, InterruptedException {
    workoutPlans = getWorkoutPlans();
    savedWorkoutPlans = getSavedWorkoutPlans();
    parameters =
        "{\"date-time\": { \"startDateTime\": \"2020-08-02T19:00:18-07:00\", \"endDateTime\": \"2020-08-08T19:00:18-07:00\" }, \"workout-type\": \"HIIT\"}";

    for (WorkoutPlan workoutPlan : workoutPlans) {
      workoutTester.setWorkoutPlan(workoutPlan);
      workoutTester.setParameters("workout.plan", parameters);
    }

    List<Entity> storedEntities = workoutTester.fetchDatastoreEntities("WorkoutPlan", "user1");

    // Saving workout plans
    for (WorkoutPlan planToSave : savedWorkoutPlans) {
      String userId = planToSave.getUserId();
      int workoutPlanId = planToSave.getWorkoutPlanId();
      String workoutPlanString =
          "{\"userId\":" + "\"" + userId + "\"" + ",\"workoutPlanId\":" + workoutPlanId + "}";
      workoutTester.setSavedWorkoutPlan(workoutPlanString);
      workoutTester.saveWorkoutPlan();
    }

    ArrayList<Integer> savedWorkoutPlanIdList = getWorkoutPlanIds(savedWorkoutPlans);

    // Assertions
    ArrayList<JSONObject> savedWorkoutPlansFromServlet = workoutTester.getSavedWorkoutPlans();
    for (JSONObject workoutPlanJson : savedWorkoutPlansFromServlet) {
      assertTrue(savedWorkoutPlanIdList.contains(workoutPlanJson.get("workoutPlanId")));
    }

    assertNotNull(workoutTester.getDisplay());
  }

  /**
   * Testing output when user specifies all possible parameters to create workout plan for 28 days
   */
  @Test
  public void testWorkoutPlan28Days() throws InvalidProtocolBufferException, IOException {

    TestHelper tester =
        new TestHelper(
            // User input text
            "Make me a 28 day ab workout plan",
            // Parameter JSON string
            "{\"date-time\": { \"startDateTime\": \"2020-08-02T17:53:33-07:00\", \"endDateTime\": \"2020-08-30T17:53:33-07:00\" }, \"workout-type\": \"abs\"}",
            // Intent returned from Dialogflow
            "workout.plan");

    Output output = tester.getOutput();

    // Assertions
    assertEquals("Here is your 28 day abs workout plan:", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
  }

  /**
   * Testing output when user specifies all possible parameters to create workout plan for 2 weeks
   */
  @Test
  public void testWorkoutPlan2Weeks() throws InvalidProtocolBufferException, IOException {

    TestHelper tester =
        new TestHelper(
            // User input text
            "Plan me a 2 week full body workout schedule",
            // Parameter JSON string
            "{\"date-time\": { \"startDateTime\": \"2020-08-02T17:04:42-07:00\", \"endDateTime\": \"2020-08-16T17:04:42-07:00\" }, \"workout-type\": \"full body\"}",
            // Intent returned from Dialogflow
            "workout.plan");

    Output output = tester.getOutput();

    // Assertions
    assertEquals("Here is your 14 day full body workout plan:", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
  }

  /**
   * Testing output when user specifies all possible parameters to create workout plan for 1 month
   */
  @Test
  public void testWorkoutPlan1Month() throws InvalidProtocolBufferException, IOException {

    TestHelper tester =
        new TestHelper(
            // User input text
            "Make me an ab workout plan for 1 month",
            // Parameter JSON string
            "{\"date-time\": { \"endDateTime\": \"2020-09-01T17:46:46-07:00\", \"startDateTime\": \"2020-08-02T17:46:46-07:00\" }, \"workout-type\": \"abs\"}",
            // Intent returned from Dialogflow
            "workout.plan");

    Output output = tester.getOutput();

    // Assertions
    assertEquals("Here is your 30 day abs workout plan:", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
  }

  private static ArrayList<WorkoutPlan> getWorkoutPlans() {
    ArrayList<WorkoutPlan> workoutPlans = new ArrayList<>();
    workoutPlans.add(new WorkoutPlan("user1", "planName1", getVideosList(), 1, "dateCreated1", 6));
    workoutPlans.add(new WorkoutPlan("user1", "planName2", getVideosList(), 2, "dateCreated2", 6));
    workoutPlans.add(new WorkoutPlan("user1", "planName3", getVideosList(), 3, "dateCreated3", 6));
    workoutPlans.add(new WorkoutPlan("user1", "planName4", getVideosList(), 4, "dateCreated4", 6));
    workoutPlans.add(new WorkoutPlan("user1", "planName5", getVideosList(), 5, "dateCreated5", 6));
    workoutPlans.add(new WorkoutPlan("user1", "planName6", getVideosList(), 6, "dateCreated6", 6));
    return workoutPlans;
  }

  /** Creates and returns list of mock workout plan to save */
  private static ArrayList<WorkoutPlan> getSavedWorkoutPlans() {
    ArrayList<WorkoutPlan> savedWorkoutPlans = new ArrayList<>();
    savedWorkoutPlans.add(
        new WorkoutPlan("user1", "planName2", getVideosList(), 2, "dateCreated2", 6));
    savedWorkoutPlans.add(
        new WorkoutPlan("user1", "planName5", getVideosList(), 5, "dateCreated5", 6));
    savedWorkoutPlans.add(
        new WorkoutPlan("user1", "planName6", getVideosList(), 6, "dateCreated6", 6));
    return savedWorkoutPlans;
  }

  /** Create mock YouTubeVideo objects */
  private static ArrayList<ArrayList<YouTubeVideo>> getVideosList() {
    ArrayList<ArrayList<YouTubeVideo>> videosList = new ArrayList<>();

    ArrayList<YouTubeVideo> videosList1 = new ArrayList<>();
    videosList1.add(
        new YouTubeVideo(
            "user1",
            "channel0",
            "title0",
            "description0",
            "thumbnail0",
            "videoId0",
            "channelId0",
            0,
            5,
            1,
            5));
    videosList1.add(
        new YouTubeVideo(
            "user1",
            "channel1",
            "title1",
            "description1",
            "thumbnail1",
            "videoId1",
            "channelId1",
            1,
            5,
            1,
            5));
    videosList.add(videosList1);

    ArrayList<YouTubeVideo> videosList2 = new ArrayList<>();
    videosList2.add(
        new YouTubeVideo(
            "user1",
            "channel2",
            "title2",
            "description2",
            "thumbnail2",
            "videoId2",
            "channelId2",
            2,
            5,
            1,
            5));
    videosList2.add(
        new YouTubeVideo(
            "user1",
            "channel3",
            "title3",
            "description3",
            "thumbnail3",
            "videoId3",
            "channelId3",
            3,
            5,
            1,
            5));
    videosList.add(videosList2);

    ArrayList<YouTubeVideo> videosList3 = new ArrayList<>();
    videosList3.add(
        new YouTubeVideo(
            "user1",
            "channel4",
            "title4",
            "description4",
            "thumbnail4",
            "videoId4",
            "channelId4",
            4,
            5,
            1,
            5));
    videosList3.add(
        new YouTubeVideo(
            "user1",
            "channel4",
            "title4",
            "description4",
            "thumbnail4",
            "videoId4",
            "channelId4",
            4,
            5,
            1,
            5));
    videosList.add(videosList3);

    return videosList;
  }

  /** Create list of workoutPlanIds */
  private static ArrayList<Integer> getWorkoutPlanIds(ArrayList<WorkoutPlan> workoutPlans) {
    ArrayList<Integer> workoutPlanIdList = new ArrayList<>();
    for (WorkoutPlan workoutPlan : workoutPlans) {
      workoutPlanIdList.add(workoutPlan.getWorkoutPlanId());
    }
    return workoutPlanIdList;
  }
}
