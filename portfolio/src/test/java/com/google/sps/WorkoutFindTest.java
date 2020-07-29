package com.google.sps.servlets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.maps.errors.ApiException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.sps.data.Output;
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
public class WorkoutFindTest {

  private WorkoutTestHelper workoutTester;
  private ArrayList<YouTubeVideo> videos;
  private ArrayList<YouTubeVideo> savedVideos;
  private String parameters;
  private static Logger log = LoggerFactory.getLogger(WorkoutFindTest.class);

  @Before
  public void setUp() {
    workoutTester = new WorkoutTestHelper();
  }

  @After
  public void tearDown() {
    workoutTester.tearDown();
  }

  /**
   * Checks if all YouTubeVideos from search results are stored in datastore if user is logged in
   */
  @Test
  public void testWorkoutVideoStoringWhenUserLoggedIn()
      throws IOException, InvalidProtocolBufferException, ApiException, InterruptedException {

    videos = getVideosList();
    parameters =
        "{\"duration\": {\"unit\": \"min\", \"amount\": 30 }, \"youtube-channel\": \"channel0\", \"workout-type\": \"HIIT\"}";
    workoutTester.setVideoList(videos);

    // Storing videos when creating agent
    workoutTester.setParameters("workout.find", parameters);

    ArrayList<String> videoIdList = getVideoIds(videos);

    // Assertions
    List<Entity> storedEntities = workoutTester.fetchDatastoreEntities("WorkoutVideo", "user1");

    for (Entity workoutVideoEntity : storedEntities) {
      Blob workoutVideoBlob = (Blob) workoutVideoEntity.getProperty("workoutVideo");
      YouTubeVideo workoutVideo = SerializationUtils.deserialize(workoutVideoBlob.getBytes());
      assertTrue(videoIdList.contains(workoutVideo.getVideoId()));
    }

    assertNotNull(workoutTester.getDisplay());
  }

  /** Checks that search results are not stored in datastore if user is not logged in */
  @Test
  public void testWorkoutVideoStoringWhenUserLoggedOut()
      throws IOException, InvalidProtocolBufferException, ApiException, InterruptedException {
    workoutTester.setLoggedOut();
    videos = getVideosList();
    parameters =
        "{\"duration\": {\"unit\": \"min\", \"amount\": 30 }, \"youtube-channel\": \"channel1\", \"workout-type\": \"HIIT\"}";
    workoutTester.setVideoList(videos);
    workoutTester.setParameters("workout.find", parameters);

    // Assertions
    List<Entity> storedEntities = workoutTester.fetchDatastoreEntities("WorkoutVideo", "user1");
    int numStoredWorkoutVideoEntities = storedEntities.size();
    assertEquals(0, numStoredWorkoutVideoEntities);
    assertNotNull(workoutTester.getDisplay());
  }

  /**
   * Checks if YouTubeVideos specifically saved by user are stored in datastore if user logged in
   */
  @Test
  public void testWorkoutVideoSavingWhenUserLoggedIn()
      throws IOException, InvalidProtocolBufferException, ApiException, InterruptedException {
    videos = getVideosList();
    savedVideos = getSavedVideosList();
    parameters =
        "{\"duration\": {\"unit\": \"min\", \"amount\": 30 }, \"youtube-channel\": \"\", \"workout-type\": \"HIIT\"}";
    workoutTester.setVideoList(videos);
    workoutTester.setParameters("workout.find", parameters);

    // Saving videos
    for (YouTubeVideo videoToSave : savedVideos) {
      String userId = videoToSave.getUserId();
      String videoId = videoToSave.getVideoId();
      String videoString =
          "{\"userId\":" + "\"" + userId + "\"" + ",\"videoId\":" + "\"" + videoId + "\"" + "}";
      workoutTester.setSavedVideo(videoString);
      workoutTester.saveVideo();
    }

    ArrayList<String> savedVideoIdList = getVideoIds(savedVideos);

    // Assertions
    ArrayList<JSONObject> savedVideosFromServlet = workoutTester.getSavedVideos();
    for (JSONObject videoJson : savedVideosFromServlet) {
      assertTrue(savedVideoIdList.contains(videoJson.get("videoId")));
    }

    assertNotNull(workoutTester.getDisplay());
  }

  /** Testing output when user specifies all possible parameters */
  @Test
  public void testWorkoutFind() throws InvalidProtocolBufferException, IOException {

    TestHelper tester =
        new TestHelper(
            // User input text
            "Find me 30 minute HIIT workouts from Madfit",
            // Parameter JSON string
            "{\"duration\": {\"unit\": \"min\", \"amount\": 30 }, \"youtube-channel\": \"MadFit\", \"workout-type\": \"HIIT\"}",
            // Intent returned from Dialogflow
            "workout.find");

    Output output = tester.getOutput();

    // Assertions
    assertEquals(
        "Here are videos for: 30 min HIIT workouts from MadFit", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
  }

  /** Testing output when user does not specify youtube-channel */
  @Test
  public void testWorkoutFindWithoutChannel() throws InvalidProtocolBufferException, IOException {

    TestHelper tester =
        new TestHelper(
            // User input text
            "Search for some 15 minute ab workouts",
            // Parameter JSON string
            "{\"duration\": {\"unit\": \"min\", \"amount\": 15 }, \"youtube-channel\": \"\", \"workout-type\": \"abs\"}",
            // Intent returned from Dialogflow
            "workout.find");

    Output output = tester.getOutput();

    // Assertions
    assertEquals("Here are videos for: 15 min abs workouts ", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
  }

  /** Testing output when user does not specify workout-type */
  @Test
  public void testWorkoutFindWithoutWorkoutType()
      throws InvalidProtocolBufferException, IOException {

    TestHelper tester =
        new TestHelper(
            // User input text
            "45 minute workouts from Popsugar Fitness",
            // Parameter JSON string
            "{\"duration\": {\"unit\": \"min\", \"amount\": 45 }, \"youtube-channel\": \"POPSUGAR Fitness\", \"workout-type\": \"\"}",
            // Intent returned from Dialogflow
            "workout.find");

    Output output = tester.getOutput();

    // Assertions
    assertEquals(
        "Here are videos for: 45 min workouts from POPSUGAR Fitness", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
  }

  /** Testing output when user does not specify duration */
  @Test
  public void testWorkoutFindWithoutDuration() throws InvalidProtocolBufferException, IOException {

    TestHelper tester =
        new TestHelper(
            // User input text
            "Find me ab workouts from Blogilates",
            // Parameter JSON string
            "{\"duration\": \"\", \"youtube-channel\": \"Blogilates\", \"workout-type\": \"ab\"}",
            // Intent returned from Dialogflow
            "workout.find");

    Output output = tester.getOutput();

    // Assertions
    assertEquals("Here are videos for: ab workouts from Blogilates", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
  }

  /** Testing output when user only specifies youtube-channel */
  @Test
  public void testWorkoutFindWithChannelOnly() throws InvalidProtocolBufferException, IOException {

    TestHelper tester =
        new TestHelper(
            // User input text
            "I want workouts by Pamela Reif",
            // Parameter JSON string
            "{\"duration\": \"\", \"youtube-channel\": \"Pamela Reif\", \"workout-type\": \"\"}",
            // Intent returned from Dialogflow
            "workout.find");

    Output output = tester.getOutput();

    // Assertions
    assertEquals("Here are videos for: workouts from Pamela Reif", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
  }

  /** Testing output when user only specifies workout-type */
  @Test
  public void testWorkoutFindWithWorkoutTypeOnly()
      throws InvalidProtocolBufferException, IOException {

    TestHelper tester =
        new TestHelper(
            // User input text
            "Find me tabata workouts",
            // Parameter JSON string
            "{\"duration\": \"\", \"youtube-channel\": \"\", \"workout-type\": \"tabata\"}",
            // Intent returned from Dialogflow
            "workout.find");

    Output output = tester.getOutput();

    // Assertions
    assertEquals("Here are videos for: tabata workouts ", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
  }

  /** Testing output when user only specifies duration */
  @Test
  public void testWorkoutFindWithDurationOnly() throws InvalidProtocolBufferException, IOException {

    TestHelper tester =
        new TestHelper(
            // User input text
            "Find me 45 minute workouts",
            // Parameter JSON string
            "{\"duration\": {\"unit\": \"min\", \"amount\": 45 }, \"youtube-channel\": \"\", \"workout-type\": \"\"}",
            // Intent returned from Dialogflow
            "workout.find");

    Output output = tester.getOutput();

    // Assertions
    assertEquals("Here are videos for: 45 min workouts ", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
  }

  /** Create mock YouTube Video objects */
  private static ArrayList<YouTubeVideo> getVideosList() {
    ArrayList<YouTubeVideo> videos = new ArrayList<>();
    videos.add(
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
    videos.add(
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
    videos.add(
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
    videos.add(
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
    videos.add(
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
    return videos;
  }

  /** Create mock YouTube Video objects to be saved */
  private static ArrayList<YouTubeVideo> getSavedVideosList() {
    ArrayList<YouTubeVideo> videos = new ArrayList<>();
    videos.add(
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
    videos.add(
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
    return videos;
  }

  /** Create list of videoIds */
  private static ArrayList<String> getVideoIds(ArrayList<YouTubeVideo> videos) {
    ArrayList<String> videoIdList = new ArrayList<>();
    for (YouTubeVideo video : videos) {
      videoIdList.add(video.getVideoId());
    }
    return videoIdList;
  }
}
