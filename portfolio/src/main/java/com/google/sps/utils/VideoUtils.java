package com.google.sps.utils;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VideoUtils {

  private static String URL;
  private static String maxResults;
  private static String order;
  private static String q;
  private static String type;
  private static String key;
  private static String playlistId;
  private static ArrayList<YouTubeVideo> playlistVids;
  private static ArrayList<ArrayList<YouTubeVideo>> listOfPlaylists;
  private static int randomInt;
  private static final int videosDisplayedTotal = 25;
  private static final int videosDisplayedPerPage = 5;
  private static YouTubeVideo video;
  private static String channelTitle;
  private static String title;
  private static String description;
  private static String thumbnail;
  private static String videoId;
  private static String channelId;
  private static int currentPage = 0;
  private static int totalPages = videosDisplayedTotal / videosDisplayedPerPage;

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  /**
   * Creates JSON object from url passed in from getJSONObject
   *
   * @param url for YouTube Data API search by keyword
   * @return JSONObject json from YouTube Data API search URL
   */
  private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
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
   * Sets YouTube Data API search by keyword parameters, creates URL, and passes URL into
   * readJsonFromURL
   *
   * @param workoutLength for workout video length
   * @param workoutType for workout video/playlist muscle/type
   * @param youtubeChannel for workout channel
   * @param numVideosSearched for number of videos to get from search
   * @param searchType type of search on YouTube (video or playlist)
   * @return ArrayList<YouTubeVideo> videoList list of YouTube videos
   */
  public static ArrayList<YouTubeVideo> getVideoList(
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
    return createVideoList(json, searchType);
  }

  /**
   * Sets YouTube Data API search by keyword parameters, creates URL, and passes URL into
   * readJsonFromURL
   *
   * @param planLength for workout plan length
   * @param workoutType for workout video/playlist muscle/type
   * @param searchType type of search on YouTube (video or playlist)
   * @return ArrayList<YouTubeVideo> videoList list of YouTube videos for playlist
   */
  public static ArrayList<YouTubeVideo> getPlaylistVideoList(
      int maxPlaylistResults, int planLength, String workoutType, String searchType)
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
      playlistVids =
          createPlaylistVideosList(json, searchType, maxPlaylistResults, planLength, randomInt);
      listOfPlaylists.add(playlistVids);
      randomInt = (randomInt + 1) % maxPlaylistResults;
    }

    // Sorts list of playlists by playlist size
    sortByPlaylistSize(listOfPlaylists);

    return listOfPlaylists.get(0);
  }

  /**
   * Created list of videos from JSONObject
   *
   * @param json JSONObject from YouTube Data API call
   * @return ArrayList<YouTubeVideo> list of YouTube videos
   */
  private static ArrayList<YouTubeVideo> createVideoList(JSONObject json, String searchType) {
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
  private static void setVideoParameters(String videoString) {
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
  private static void setPlaylistVideoParameters(String playlistVideoString) {
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
   * Gets playlistId from JSONObject and passes that into getPlaylistVideos
   *
   * @param json JSONObject from initial YouTube Data API call for playlists
   * @param planLength length of workout plan in days
   * @return ArrayList<YouTubeVideo> list of YouTube videos from playlist
   */
  private static ArrayList<YouTubeVideo> createPlaylistVideosList(
      JSONObject json, String searchType, int maxPlaylistResults, int planLength, int randomInt)
      throws IOException {
    JSONArray playlist = json.getJSONArray("items");
    String playlistString = new Gson().toJson(playlist.get(randomInt));
    JSONObject playlistJSONObject = new JSONObject(playlistString).getJSONObject("map");
    JSONObject id = playlistJSONObject.getJSONObject("id").getJSONObject("map");
    String playlistId = new Gson().toJson(id.get("playlistId"));
    return getPlaylistVideos(searchType, playlistId, planLength);
  }

  /**
   * Makes second call to YouTube Data API to get videos from playlist
   *
   * @param json JSONObject from initial YouTube Data API call for playlists
   * @param planLength length of workout plan in days
   * @return ArrayList<YouTubeVideo> list of YouTube videos from playlist
   */
  private static ArrayList<YouTubeVideo> getPlaylistVideos(
      String searchType, String playlistId, int planLength) throws IOException {
    String baseURL = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet";
    maxResults = setMaxResults(planLength);
    playlistId = setPlaylistID(playlistId);
    key = setKey();
    URL = setURL(baseURL, maxResults, null, playlistId, key, null);
    JSONObject json = readJsonFromUrl(URL);
    return createVideoList(json, searchType);
  }

  /** Set parameters for YouTube Data API search */
  private static String setMaxResults(int maxResultAmount) {
    return "maxResults=" + String.valueOf(maxResultAmount);
  }

  private static String setOrderRelevance() {
    return "order=relevance";
  }

  private static String setVideoQ(String workoutLength, String workoutType, String youtubeChannel) {
    return "q=" + String.join("+", workoutLength, workoutType, youtubeChannel, "workout");
  }

  private static String setPlaylistQ(int planLength, String workoutType) {
    return "q="
        + String.join("+", String.valueOf(planLength), "day", workoutType, "workout", "challenge");
  }

  private static String setPlaylistID(String playlistId) {
    return "playlistId=" + playlistId.replaceAll("\"", "");
  }

  private static String setType(String searchType) {
    return "type=" + searchType;
  }

  private static String setKey() throws IOException {
    String apiKey =
        new String(
            Files.readAllBytes(
                Paths.get(VideoUtils.class.getResource("/files/youtubeAPIKey.txt").getFile())));
    return "key=" + apiKey;
  }

  private static String setURL(
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
  public static ArrayList<ArrayList<YouTubeVideo>> partitionOfSize(
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
  private static void sortByPlaylistSize(ArrayList<ArrayList<YouTubeVideo>> listOfPlaylists) {
    Collections.sort(
        listOfPlaylists,
        new Comparator<List>() {
          public int compare(List a1, List a2) {
            return a2.size() - a1.size();
          }
        });
  }

  /** Gets random int in range [min, max) */
  private static int getRandomNumberInRange(int min, int max) {
    if (min >= max) {
      throw new IllegalArgumentException("Max must be greater than min");
    }
    return (int) (Math.random() * (max - min)) + min;
  }

  /**
   * Saves workout plan if user if logged in
   *
   * @param userID The current logged-in user's ID number
   * @param datastore Datastore instance to retrieve data from
   * @param workoutPlan ArrayList<ArrayList<<YouTubeVideo>> workout plan videos user wants to save
   */
  public static void saveWorkoutPlan(
      String userId, DatastoreService datastore, ArrayList<ArrayList<YouTubeVideo>> workoutPlan) {
    long timestamp = System.currentTimeMillis();
    Entity workoutPlanEntity = new Entity("WorkoutPlan");

    byte[] workoutPlanData = SerializationUtils.serialize(workoutPlan);
    Blob workoutPlanBlob = new Blob(workoutPlanData);

    workoutPlanEntity.setProperty("userId", userId);
    workoutPlanEntity.setProperty("workoutPlan", workoutPlanBlob);
    workoutPlanEntity.setProperty("timestamp", timestamp);
    datastore.put(workoutPlanEntity);
  }

  public static ArrayList<WorkoutPlan> getWorkoutPlans(String userId, DatastoreService datastore) {

    Filter userFilter = createUserIdFilter(userId);
    Query query =
        new Query("WorkoutPlan")
            .setFilter(userFilter)
            .addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery results = datastore.prepare(query);

    ArrayList<WorkoutPlan> workoutPlans = new ArrayList<>();
    for (Entity entity : results.asIterable()) {

      long timestamp = (long) entity.getProperty("timestamp");
      Blob workoutPlanBlob = (Blob) entity.getProperty("workoutPlan");
      WorkoutPlan workoutPlan = SerializationUtils.deserialize(workoutPlanBlob.getBytes());
      workoutPlans.add(workoutPlan);
    }

    return workoutPlans;
  }

  private static Filter createUserIdFilter(String userId) {
    return new CompositeFilter(
        CompositeFilterOperator.AND,
        Arrays.asList(new FilterPredicate("userId", FilterOperator.EQUAL, userId)));
  }
}
