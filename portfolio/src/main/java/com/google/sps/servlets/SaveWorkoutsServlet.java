package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.data.WorkoutPlan;
import com.google.sps.utils.WorkoutProfileUtils;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

@WebServlet("/save-workouts")
public class SaveWorkoutsServlet extends HttpServlet {

  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private UserService userService = UserServiceFactory.getUserService();

  /** Saves workout plans to user profile when save button is clicked */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    String workoutPlanString = request.getParameter("workout-plan");
    JSONObject workoutPlanJson = new JSONObject(workoutPlanString);
    String userId = (String) workoutPlanJson.get("userId");
    int workoutPlanId = (int) workoutPlanJson.get("workoutPlanId");

    // Getting workout plan from all stored workout plans that user wants to save
    WorkoutPlan workoutPlanToSave =
        WorkoutProfileUtils.getStoredWorkoutPlan(userId, workoutPlanId, datastore);

    // Saves workout plan
    WorkoutProfileUtils.saveWorkoutPlan(workoutPlanToSave, datastore);
  }

  /** Gets saved workouts to display on workout dashboard for specific user */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    String userId = userService.getCurrentUser().getUserId();
    ArrayList<WorkoutPlan> savedWorkoutPlans =
        WorkoutProfileUtils.getSavedWorkoutPlans(userId, datastore);
    String json = new Gson().toJson(savedWorkoutPlans);
    response.getWriter().write(json);
  }
}
