package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.data.YouTubeVideo;
import com.google.sps.utils.WorkoutProfileUtils;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

@WebServlet("/save-video")
public class SaveVideoServlet extends HttpServlet {

  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private UserService userService = UserServiceFactory.getUserService();

  /** Saves workout videos to user profile when save video button is clicked */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    String workoutVideoString = request.getParameter("workout-video");
    JSONObject workoutVideoJson = new JSONObject(workoutVideoString);
    String userId = (String) workoutVideoJson.get("userId");
    String workoutVideoId = (String) workoutVideoJson.get("videoId");

    // Getting workout plan from all stored workout plans that user wants to save
    YouTubeVideo workoutVideoToSave =
        WorkoutProfileUtils.getStoredWorkoutVideo(userId, workoutVideoId, datastore);

    // Saves workout plan
    WorkoutProfileUtils.saveWorkoutVideo(workoutVideoToSave, datastore);
  }

  /** Gets saved videos to display on workout dashboard for specific user */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    String userId = userService.getCurrentUser().getUserId();
    ArrayList<YouTubeVideo> savedWorkoutVideos =
        WorkoutProfileUtils.getSavedWorkoutVideos(userId, datastore);
    String json = new Gson().toJson(savedWorkoutVideos);
    response.getWriter().write(json);
  }
}
