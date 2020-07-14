package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.gson.Gson;
import com.google.maps.errors.ApiException;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.sps.data.YouTubeVideo;
import com.google.sps.utils.WorkoutUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workout Agent: finds relevant workout videos through Youtube and schedules workout plans. Also
 * tracks workouts for user.
 */
public class WorkoutAgent implements Agent {

  private static Logger log = LoggerFactory.getLogger(WorkoutAgent.class);

  private final String intentName;
  private String output = null;
  private String display = null;
  private String redirect = null;
  private String workoutType = "";
  private String workoutLength = "";
  private String youtubeChannel = "";
  private final int numVideosDisplayed = 5;
  private String amount = "";
  private String unit = "";
  private YouTubeVideo video;
  private String channelTitle;
  private String title;
  private String description;
  private String thumbnail;
  private String videoId;
  private String channelId;

  public WorkoutAgent(String intentName, Map<String, Value> parameters)
      throws IllegalStateException, IOException, ApiException, InterruptedException,
          ArrayIndexOutOfBoundsException {
    this.intentName = intentName;
    setParameters(parameters);
  }

  @Override
  public void setParameters(Map<String, Value> parameters)
      throws IllegalStateException, IOException, ApiException, InterruptedException,
          ArrayIndexOutOfBoundsException {
    if (intentName.contains("find")) {
      workoutFind(parameters);
    } else if (intentName.contains("schedule")) {
      workoutSchedule(parameters);
    }
  }

  @Override
  public String getOutput() {
    return output;
  }

  @Override
  public String getDisplay() {
    return display;
  }

  @Override
  public String getRedirect() {
    return redirect;
  }

  /**
   * Private workoutFind method, that displays workouts specified by user request. Method sets
   * parameters for workoutLength, workoutType, and youtubeChannel based on Dialogflow detection and
   * makes calls to set display and set output. parameters map needs to include duration struct to
   * set String workoutLength, String workoutType, and String youtubeChannel
   *
   * @param parameters parameter Map from Dialogflow
   */
  private void workoutFind(Map<String, Value> parameters) throws IOException {
    log.info(String.valueOf(parameters));

    if (parameters.get("duration").hasStructValue()) {
      Struct durationStruct = parameters.get("duration").getStructValue();
      Map<String, Value> durationMap = durationStruct.getFieldsMap();
      amount = String.valueOf(Math.round(durationMap.get("amount").getNumberValue()));
      unit = durationMap.get("unit").getStringValue();
    }

    workoutLength = amount + " " + unit;
    workoutType = parameters.get("workout-type").getStringValue();
    youtubeChannel = parameters.get("youtube-channel").getStringValue();

    // Set output
    setWorkoutFindOutput();

    // Set display
    setWorkoutFindDisplay();
  }

  /**
   * Private setworkoutFindOutput method, that sets the agent output based on set parameters for
   * workoutLength, workoutType, and youtubeChannel from workoutFind method
   */
  private void setWorkoutFindOutput() {
    output = "Here are videos for: " + workoutLength + " " + workoutType + " workouts";
    if (!youtubeChannel.equals("")) {
      output += " from " + youtubeChannel;
    }
  }

  /**
   * Private setworkoutFindDisplay method, that sets the agent display to JSON string by makifn YT
   * Data API call from WorkoutUtils to get passed into workout.js
   */
  private void setWorkoutFindDisplay() throws IOException {

    // Removing white space so search URL does not have spaces
    workoutLength = workoutLength.replaceAll("\\s", "");
    workoutType = workoutType.replaceAll("\\s", "");
    youtubeChannel = youtubeChannel.replaceAll("\\s", "");

    // Make API call to WorkoutUtils to get json object of videos
    JSONObject json =
        WorkoutUtils.getJSONObject(workoutLength, workoutType, youtubeChannel, numVideosDisplayed);
    JSONArray videos = json.getJSONArray("items");

    List<YouTubeVideo> videoList = new ArrayList<>();

    for (int i = 0; i < videos.length(); i++) {
      String videoString = new Gson().toJson(videos.get(i));
      setVideoParameters(videoString);
      video = new YouTubeVideo(channelTitle, title, description, thumbnail, videoId, channelId);
      videoList.add(video);
    }

    display = new Gson().toJson(videoList);
  }

  private void setVideoParameters(String videoString) {
    JSONObject videoJSONObject = new JSONObject(videoString).getJSONObject("map");
    JSONObject id = videoJSONObject.getJSONObject("id").getJSONObject("map");
    videoId = new Gson().toJson(id.get("videoId"));
    JSONObject snippet = videoJSONObject.getJSONObject("snippet").getJSONObject("map");
    title = new Gson().toJson(snippet.get("title"));
    description = new Gson().toJson(snippet.get("description"));
    channelTitle = new Gson().toJson(snippet.get("channelTitle"));
    channelId = new Gson().toJson(snippet.get("channelId"));
    JSONObject thumbnailJSONObject =
        snippet.getJSONObject("thumbnails").getJSONObject("map").getJSONObject("medium");
    JSONObject thumbnailURL = thumbnailJSONObject.getJSONObject("map");
    thumbnail = new Gson().toJson(thumbnailURL.get("url"));
  }

  /**
   * TODO: Private workoutSchedule method
   *
   * @param parameters parameter Map from Dialogflow
   */
  private void workoutSchedule(Map<String, Value> parameters) throws UnsupportedOperationException {
    return;
  }
}
