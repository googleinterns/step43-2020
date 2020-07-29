package com.google.sps.utils;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.users.UserService;
import com.google.gson.Gson;
import com.google.sps.data.WorkoutPlan;
import com.google.sps.data.YouTubeVideo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.WordUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VideoUtils {

  private String URL;
  private String maxResults;
  private String order;
  private String q;
  private String type;
  private String key;
  private WorkoutPlan workoutPlan;
  private String playlistId;
  private ArrayList<YouTubeVideo> playlistVids;
  private ArrayList<ArrayList<YouTubeVideo>> listOfPlaylists;
  private Map<ArrayList<YouTubeVideo>, String> playlistToId;
  private int randomInt;
  private final int videosDisplayedTotal = 25;
  private final int videosDisplayedPerPage = 5;
  private YouTubeVideo video;
  private String channelTitle;
  private String title;
  private String description;
  private String thumbnail;
  private String videoId;
  private String channelId;
  private int currentPage = 0;
  private int totalPages = videosDisplayedTotal / videosDisplayedPerPage;
  private WorkoutProfileUtils workoutProfileUtils = new WorkoutProfileUtils();

  /**
   * Sets YouTube Data API search by keyword parameters, creates URL, and passes URL into
   * readJsonFromURL
   *
   * @param userService UserService to get userId if user is logged in
   * @param workoutLength for workout video length
   * @param workoutType for workout video/playlist muscle/type
   * @param youtubeChannel for workout channel
   * @param numVideosSearched for number of videos to get from search
   * @param searchType type of search on YouTube (video or playlist)
   * @return ArrayList<YouTubeVideo> videoList list of YouTube videos
   */
  public ArrayList<YouTubeVideo> getVideoList(
      UserService userService,
      String workoutLength,
      String workoutType,
      String youtubeChannel,
      int numVideosSearched,
      String searchType)
      throws IOException, JSONException {

    String baseURL = "https://www.googleapis.com/youtube/v3/search?part=snippet";
    maxResults = setMaxResults(numVideosSearched);
    order = setOrderRelevance();
    q = setVideoQ(workoutLength, workoutType, youtubeChannel);
    type = setType(searchType);
    key = setKey();
    URL = setURL(baseURL, maxResults, order, q, type, key);
    JSONObject json = readJsonFromUrl(URL);
    return createVideoList(userService, json, searchType);
  }

  /**
   * Gets WorkoutPlan depending on user's workout plan requirements
   *
   * @param userService UserService to get userId if user is logged in
   * @param datastore DatastoreService to get workoutPlanId
   * @param maxPlayListResults number of playlists to search for
   * @param planLength for workout plan length
   * @param workoutType for workout video/playlist muscle/type
   * @param searchType type of search on YouTube (video or playlist)
   * @return WorkoutPlan with list of videos and playlistId if user is not logged in and list of
   *     videos, userId, workoutPlanName, workoutPlanId, dateCreated, and planLength if user logged
   *     in
   */
  public WorkoutPlan getWorkoutPlan(
      UserService userService,
      DatastoreService datastore,
      int maxPlaylistResults,
      int planLength,
      String workoutType,
      String searchType)
      throws IOException, JSONException {

    // Initialize map to populate with function call to getPlayistVideoList
    playlistToId = new HashMap<>();

    // Get workout plan playlist with user specified parameters
    ArrayList<ArrayList<YouTubeVideo>> listOfVideoLists =
        getPlaylistVideoList(userService, maxPlaylistResults, planLength, workoutType, "playlist");

    // Creating WorkoutPlan object with extra parameters if user logged in
    if (userService.isUserLoggedIn()) {

      String userId = userService.getCurrentUser().getUserId();

      // Capitalize first letter of each word in workoutType string
      workoutType = WordUtils.capitalize(workoutType, null);
      String workoutPlanName = String.valueOf(planLength) + " Day " + workoutType + " Workout Plan";
      int workoutPlanId = workoutProfileUtils.getWorkoutPlanId(userId, datastore);

      // Create formatted dateCreated String
      SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");
      Date date = new Date(System.currentTimeMillis());
      String dateCreated = "Created: " + formatter.format(date).toString();

      workoutPlan =
          new WorkoutPlan(
              userId, workoutPlanName, listOfVideoLists, workoutPlanId, dateCreated, planLength);
    } else {
      workoutPlan = new WorkoutPlan(listOfVideoLists, playlistId);
    }

    return workoutPlan;
  }

  /**
   * Sets YouTube Data API search by keyword parameters, creates URL, and passes URL into
   * readJsonFromURL to search for playlist
   *
   * @param userService UserService to get userId if user is logged in
   * @param maxPlayListResults number of playlists to search for
   * @param planLength for workout plan length
   * @param workoutType for workout video/playlist muscle/type
   * @param searchType type of search on YouTube (video or playlist)
   * @return ArrayList<ArrayList<YouTubeVideo>> list of lists of videos in playlist
   */
  private ArrayList<ArrayList<YouTubeVideo>> getPlaylistVideoList(
      UserService userService,
      int maxPlaylistResults,
      int planLength,
      String workoutType,
      String searchType)
      throws IOException, JSONException {
    String baseURL = "https://www.googleapis.com/youtube/v3/search?part=snippet";
    maxResults = setMaxResults(maxPlaylistResults);
    order = setOrderRelevance();
    q = setPlaylistQ(planLength, workoutType);
    type = setType(searchType);
    key = setKey();
    URL = setURL(baseURL, maxResults, order, q, type, key);
    JSONObject json = readJsonFromUrl(URL);

    // First finds random playlist and then adds rest of playlists to the listOfPlaylists
    // Finding random so when the list gets sorted by size, if the first random playlist found had
    // the correct number of videos, it will get returned
    // If the first playlist found did not have the correct number of videos, it will return the
    // next playlist with the correct number of videos (aka the playlist with the largest amount of
    // videos)
    randomInt = getRandomNumberInRange(0, maxPlaylistResults);
    listOfPlaylists = new ArrayList<>();
    for (int i = 0; i < maxPlaylistResults; i++) {
      playlistVids = createPlaylistVideosList(userService, json, searchType, planLength, randomInt);
      listOfPlaylists.add(playlistVids);
      randomInt = (randomInt + 1) % maxPlaylistResults;
    }

    // Sorts list of playlists by playlist size
    sortByPlaylistSize(listOfPlaylists);

    // Gets the first playlist from the list of playlists
    // Breaks up the ArrayList into chunks of 5 and puts that into one ArrayList to make an
    // ArrayList of ArrayLists
    ArrayList<YouTubeVideo> playlistVideos = listOfPlaylists.get(0);
    playlistId = playlistToId.get(playlistVideos);
    return partitionOfSize(playlistVideos, 5);
  }

  /**
   * Created list of videos from JSONObject
   *
   * @param userService UserService to get userId if user is logged in
   * @param json JSONObject from YouTube Data API call
   * @param searchType string to call correct function to set paramters depending on video or
   *     playlist
   * @return ArrayList<YouTubeVideo> list of YouTube videos
   */
  private ArrayList<YouTubeVideo> createVideoList(
      UserService userService, JSONObject json, String searchType) {
    JSONArray videos = json.getJSONArray("items");

    ArrayList<YouTubeVideo> videoList = new ArrayList<>();

    for (int index = 0; index < videos.length(); index++) {
      String videoString = new Gson().toJson(videos.get(index));
      if (searchType.equals("video")) {
        setVideoParameters(videoString);
      } else if (searchType.equals("playlist")) {
        setPlaylistVideoParameters(videoString);
      }

      if (index % videosDisplayedPerPage == 0) {
        currentPage += 1;
      }

      if (userService.isUserLoggedIn()) {
        String userId = userService.getCurrentUser().getUserId();
        video =
            new YouTubeVideo(
                userId,
                channelTitle,
                title,
                description,
                thumbnail,
                videoId,
                channelId,
                index,
                videosDisplayedPerPage,
                currentPage,
                totalPages);
      } else {
        video =
            new YouTubeVideo(
                channelTitle,
                title,
                description,
                thumbnail,
                videoId,
                channelId,
                index,
                videosDisplayedPerPage,
                currentPage,
                totalPages);
      }
      videoList.add(video);
    }

    return videoList;
  }

  /**
   * Sets parameters: channelTitle, title, description, thumbnail, videoId, channelId for YouTube
   * video object
   *
   * @param videoString JSON string of YouTube video from API call
   */
  private void setVideoParameters(String videoString) {
    // Assigning correct values from JSONObject to string
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
   * Sets parameters: channelTitle, title, description, thumbnail, videoId, channelId for YouTube
   * video object from playlists
   *
   * @param playlistVideoString JSON string of YouTube videos in playlist from API call
   */
  private void setPlaylistVideoParameters(String playlistVideoString) {
    // Set parameters from JSONObject
    JSONObject videoJSONObject = new JSONObject(playlistVideoString).getJSONObject("map");
    JSONObject snippet = videoJSONObject.getJSONObject("snippet").getJSONObject("map");
    title = new Gson().toJson(snippet.get("title"));
    description = new Gson().toJson(snippet.get("description"));
    channelTitle = new Gson().toJson(snippet.get("channelTitle"));
    channelId = new Gson().toJson(snippet.get("channelId"));
    JSONObject thumbnailJSONObject =
        snippet.getJSONObject("thumbnails").getJSONObject("map").getJSONObject("medium");
    JSONObject thumbnailURL = thumbnailJSONObject.getJSONObject("map");
    thumbnail = new Gson().toJson(thumbnailURL.get("url"));
    JSONObject resourceId = snippet.getJSONObject("resourceId").getJSONObject("map");
    videoId = new Gson().toJson(resourceId.get("videoId"));
  }

  /**
   * Gets playlistId from JSONObject and passes that into getPlaylistVideos to create list of videos
   *
   * @param userService UserService to get userId if user is logged in
   * @param json JSONObject from initial YouTube Data API call for playlists
   * @param searchType type of search to pass into getPlaylistVideos
   * @param planLength length of workout plan in days
   * @param randomInt random int to ensure user gets different workout plans each time
   * @return ArrayList<YouTubeVideo> list of YouTube videos from playlist
   */
  private ArrayList<YouTubeVideo> createPlaylistVideosList(
      UserService userService, JSONObject json, String searchType, int planLength, int randomInt)
      throws IOException {

    // Set parameters from JSONObject
    JSONArray playlist = json.getJSONArray("items");
    String playlistString = new Gson().toJson(playlist.get(randomInt));
    JSONObject playlistJSONObject = new JSONObject(playlistString).getJSONObject("map");
    JSONObject id = playlistJSONObject.getJSONObject("id").getJSONObject("map");
    String playlistId = new Gson().toJson(id.get("playlistId")).replaceAll("^\"+|\"+$", "");

    // Make function call to get videos in playlist
    ArrayList<YouTubeVideo> playlistVideos =
        getPlaylistVideos(userService, searchType, playlistId, planLength);

    // Store playlist and playlistId in map
    playlistToId.put(playlistVideos, playlistId);

    return playlistVideos;
  }

  /**
   * Makes second call to YouTube Data API to get videos from playlist
   *
   * @param userService UserService to get userId if user is logged in
   * @param searchType type of search to pass into createVideoList
   * @param playlistId playlistId to get videos from this playlist
   * @param planLength length of workout plan in days
   * @return ArrayList<YouTubeVideo> list of YouTube videos from playlist
   */
  private ArrayList<YouTubeVideo> getPlaylistVideos(
      UserService userService, String searchType, String playlistId, int planLength)
      throws IOException {
    String baseURL = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet";
    maxResults = setMaxResults(planLength);
    playlistId = setPlaylistID(playlistId);
    key = setKey();
    URL = setURL(baseURL, maxResults, null, playlistId, key, null);
    JSONObject json = readJsonFromUrl(URL);
    return createVideoList(userService, json, searchType);
  }

  /** Set parameters for YouTube Data API search */
  private String setMaxResults(int maxResultAmount) {
    return "maxResults=" + String.valueOf(maxResultAmount);
  }

  private String setOrderRelevance() {
    return "order=relevance";
  }

  private String setVideoQ(String workoutLength, String workoutType, String youtubeChannel) {
    return "q=" + String.join("+", workoutLength, workoutType, youtubeChannel, "workout");
  }

  private String setPlaylistQ(int planLength, String workoutType) {
    return "q="
        + String.join("+", String.valueOf(planLength), "day", workoutType, "workout", "challenge");
  }

  private String setPlaylistID(String playlistId) {
    return "playlistId=" + playlistId.replaceAll("\"", "");
  }

  private String setType(String searchType) {
    return "type=" + searchType;
  }

  private String setKey() throws IOException {
    String apiKey =
        new String(
            Files.readAllBytes(
                Paths.get(VideoUtils.class.getResource("/files/youtubeAPIKey.txt").getFile())));
    return "key=" + apiKey;
  }

  private String setURL(
      String baseURL, String maxResults, String order, String q, String type, String key) {
    return String.join("&", baseURL, maxResults, order, q, type, key);
  }

  /**
   * Splits ArrayList of YouTubeVideos into chunks of ArrayLists of YouTube videos to return an
   * ArrayList of ArrayLists This helps make the frontend code to display workout plans much less
   * repetitive
   *
   * @param videoList ArrayList of all YouTube videos in workout plan
   * @param chunkSize int of how large the smaller ArrayLists should be
   * @return ArrayList<ArrayList<YouTubeVideo> ArrayList of ArrayLists of size at most chunkSize
   *     containing workout plan YouTubeVideos
   */
  public ArrayList<ArrayList<YouTubeVideo>> partitionOfSize(
      ArrayList<YouTubeVideo> videoList, int chunkSize) {
    ArrayList<ArrayList<YouTubeVideo>> listOfLists = new ArrayList<>();
    int startIndex = 0;
    int endIndex = chunkSize;
    int listSize = videoList.size();
    int numLists;
    if (listSize % 5 != 0) {
      numLists = listSize / chunkSize + 1;
    } else {
      numLists = listSize / chunkSize;
    }

    for (int i = 0; i < numLists; i++) {
      ArrayList<YouTubeVideo> chunkedList = new ArrayList(videoList.subList(startIndex, endIndex));
      listOfLists.add(chunkedList);
      startIndex += chunkSize;

      if (endIndex + chunkSize > listSize) {
        endIndex = listSize;
      } else {
        endIndex += chunkSize;
      }
    }

    return listOfLists;
  }

  /**
   * Sorts playlists by number of videos in playlist to ensure the right amount of playlist videos
   * get returned
   *
   * @param listOfPlaylists List of lists that need to be sorted by size
   */
  private void sortByPlaylistSize(ArrayList<ArrayList<YouTubeVideo>> listOfPlaylists) {
    Collections.sort(
        listOfPlaylists,
        new Comparator<List>() {
          public int compare(List a1, List a2) {
            return a2.size() - a1.size();
          }
        });
  }

  /**
   * Creates JSON object from url passed in from getJSONObject
   *
   * @param url for YouTube Data API search by keyword
   * @return JSONObject json from YouTube Data API search URL
   */
  private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
    InputStream is = new URL(url).openStream();
    try {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      String jsonText = readAll(rd);
      JSONObject json = new JSONObject(jsonText);
      return json;
    } finally {
      is.close();
    }
  }

  /**
   * Creates JSON string by reading from URL text
   *
   * @param rd Reader that is created in call to readJsonFromUrl
   * @return json String that can be made into a JSONObject
   */
  private String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  /** Gets random int in range [min, max) */
  private int getRandomNumberInRange(int min, int max) {
    if (min >= max) {
      throw new IllegalArgumentException("Max must be greater than min");
    }
    return (int) (Math.random() * (max - min)) + min;
  }
}
