/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/save-video")
public class SaveVideoServlet extends HttpServlet {
  private static Logger log = LoggerFactory.getLogger(SaveVideoServlet.class);
  private DatastoreService datastore = createDatastore();
  private UserService userService = createUserService();

  /** Saves workout videos to user profile when save video button is clicked */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    String workoutVideoString = request.getParameter("workout-video");
    JSONObject workoutVideoJson = new JSONObject(workoutVideoString);
    String userId = (String) workoutVideoJson.get("userId");
    String workoutVideoId = (String) workoutVideoJson.get("videoId");

    // Getting workout plan from all stored workout plans that user wants to save
    WorkoutProfileUtils workoutProfileUtils = new WorkoutProfileUtils();
    YouTubeVideo workoutVideoToSave =
        workoutProfileUtils.getStoredWorkoutVideo(userId, workoutVideoId, datastore);

    // Saves workout plan
    workoutProfileUtils.saveWorkoutVideo(workoutVideoToSave, datastore);
  }

  /** Gets saved videos to display on workout dashboard for specific user */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    String userId = userService.getCurrentUser().getUserId();

    // Getting all saved workout videos saving them response
    WorkoutProfileUtils workoutProfileUtils = new WorkoutProfileUtils();
    ArrayList<YouTubeVideo> savedWorkoutVideos =
        workoutProfileUtils.getSavedWorkoutVideos(userId, datastore);
    String json = new Gson().toJson(savedWorkoutVideos);
    response.getWriter().write(json);
  }

  /** Creates UserService instance */
  protected UserService createUserService() {
    return UserServiceFactory.getUserService();
  }

  /** Creates DatastoreService instance */
  protected DatastoreService createDatastore() {
    return DatastoreServiceFactory.getDatastoreService();
  }
}
