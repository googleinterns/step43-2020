package com.google.sps.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONException;
import org.json.JSONObject;

public class WorkoutUtils {

  private static String URL;
  private static String maxResults;
  private static String q;
  private static String type;
  private static String key;

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

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

  public static JSONObject getJSONObject(
      String workoutLength, String workoutType, String youtubeChannel, int numVideos)
      throws IOException, JSONException {

    maxResults = setMaxResults(numVideos);
    q = setQ(workoutLength, workoutType, youtubeChannel);
    type = setType();
    key = setKey();
    URL = setURL(maxResults, q, type, key);
    JSONObject json = readJsonFromUrl(URL);
    return json;
  }

  private static String setMaxResults(int numVideos) {
    return "maxResults=" + String.valueOf(numVideos);
  }

  private static String setQ(String workoutLength, String workoutType, String youtubeChannel) {
    return "q=" + String.join("+", workoutLength, workoutType, youtubeChannel, "workout");
  }

  private static String setType() {
    return "type=video";
  }

  private static String setKey() throws IOException {
    String apiKey =
        new String(
            Files.readAllBytes(
                Paths.get(WorkoutUtils.class.getResource("/files/youtubeAPIKey.txt").getFile())));
    return "key=" + apiKey;
  }

  private static String setURL(String maxResults, String q, String type, String key) {
    String baseURL = "https://www.googleapis.com/youtube/v3/search?part=snippet";
    return String.join("&", baseURL, maxResults, q, type, key);
  }
}
