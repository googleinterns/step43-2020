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
import com.google.common.collect.Iterables;
import com.google.sps.data.WorkoutPlan;
import com.google.sps.data.YouTubeVideo;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.SerializationUtils;

public class WorkoutProfileUtils {

  /**
   * Stores all generated workout plans if user is logged in
   *
   * @param datastore Datastore instance to store WorkoutPlan in database
   * @param workoutPlan WorkoutPlan object created by current user to store in database
   */
  public static void storeWorkoutPlan(DatastoreService datastore, WorkoutPlan workoutPlan) {

    // Transform WorkoutPlan to Blob to be able to store with Datastore
    byte[] workoutPlanData = SerializationUtils.serialize(workoutPlan);
    Blob workoutPlanBlob = new Blob(workoutPlanData);

    // Create WorkoutPlan Entity and store in Datastore
    Entity workoutPlanEntity = new Entity("WorkoutPlan");
    workoutPlanEntity.setProperty("userId", workoutPlan.getUserId());
    workoutPlanEntity.setProperty("workoutPlan", workoutPlanBlob);
    workoutPlanEntity.setProperty("workoutPlanId", workoutPlan.getWorkoutPlanId());
    datastore.put(workoutPlanEntity);
  }

  /**
   * Retrieves stored WorkoutPlan that user wants to save
   *
   * @param userID The current logged-in user's ID number
   * @param workoutPlanId The id for the workout they want to save
   * @param datastore Datastore instance to retrieve WorkoutPlan from database
   * @return WorkoutPlan that user wants to save
   */
  public static WorkoutPlan getStoredWorkoutPlan(
      String userId, int workoutPlanId, DatastoreService datastore) {

    // Create Filter to retrieve WorkoutPlan entities based on userId and workoutPlanId
    Filter workoutPlanFilter = createWorkoutPlanFilter(userId, workoutPlanId);
    Query query = new Query("WorkoutPlan").setFilter(workoutPlanFilter);
    PreparedQuery results = datastore.prepare(query);

    Entity workoutPlanEntity = results.asSingleEntity();

    // Transform WorkoutPlan Blob back into WorkoutPlan and return it
    Blob workoutPlanBlob = (Blob) workoutPlanEntity.getProperty("workoutPlan");
    WorkoutPlan workoutPlan = SerializationUtils.deserialize(workoutPlanBlob.getBytes());

    return workoutPlan;
  }

  /**
   * Saves WorkoutPlan when requested by user (on button click)
   *
   * @param workoutPlan WorkoutPlan object current user wants to save in database
   * @param datastore Datastore instance to save WorkoutPlan in database
   */
  public static void saveWorkoutPlan(WorkoutPlan workoutPlan, DatastoreService datastore) {

    // Transform WorkoutPlan to Blob to be able to store with Datastore
    byte[] workoutPlanData = SerializationUtils.serialize(workoutPlan);
    Blob workoutPlanBlob = new Blob(workoutPlanData);

    // Create SavedWorkoutPlan Entity and store in Datastore
    Entity savedWorkoutPlanEntity = new Entity("SavedWorkoutPlan");
    savedWorkoutPlanEntity.setProperty("userId", workoutPlan.getUserId());
    savedWorkoutPlanEntity.setProperty("workoutPlan", workoutPlanBlob);
    savedWorkoutPlanEntity.setProperty("workoutPlanId", workoutPlan.getWorkoutPlanId());
    savedWorkoutPlanEntity.setProperty("numWorkoutDaysCompleted", 0);
    datastore.put(savedWorkoutPlanEntity);
  }

  /**
   * Retrieves saved WorkoutPlans to display on user's workout dashboard
   *
   * @param userID The current logged-in user's ID number
   * @param datastore Datastore instance to retrieve WorkoutPlans from database
   * @return ArrayList of WorkoutPlan objects that has user saved
   */
  public static ArrayList<WorkoutPlan> getSavedWorkoutPlans(
      String userId, DatastoreService datastore) {

    // Create Filter to retrieve WorkoutPlan entities based on userId
    Filter savedWorkoutPlanFilter = new FilterPredicate("userId", FilterOperator.EQUAL, userId);
    Query query = new Query("SavedWorkoutPlan").setFilter(savedWorkoutPlanFilter);
    PreparedQuery results = datastore.prepare(query);

    ArrayList<WorkoutPlan> savedWorkoutPlans = new ArrayList<>();

    // For each saved WorkoutPlan, transform WorkoutPlan Blob back into WorkoutPlan and add to
    // ArrayList
    for (Entity savedWorkoutPlanEntity : results.asIterable()) {
      Blob workoutPlanBlob = (Blob) savedWorkoutPlanEntity.getProperty("workoutPlan");
      WorkoutPlan workoutPlan = SerializationUtils.deserialize(workoutPlanBlob.getBytes());
      savedWorkoutPlans.add(workoutPlan);
    }

    return savedWorkoutPlans;
  }

  /**
   * Updates SavedWorkoutPlan with the number of workout plan days the user has completed
   *
   * @param userID The current logged-in user's ID number
   * @param workoutPlanId The id for the workout they want to save
   * @param numWorkoutDaysCompleted The number of workout plan days the user has completed
   * @param datastore Datastore instance to retrieve WorkoutPlan from database
   */
  public static void updateSavedWorkoutPlan(
      String userId, int workoutPlanId, int numWorkoutDaysCompleted, DatastoreService datastore) {

    // Create Filter to retrieve SavedWorkoutPlan entities based on userId and workoutPlanId
    Filter workoutPlanFilter = createWorkoutPlanFilter(userId, workoutPlanId);
    Query query = new Query("SavedWorkoutPlan").setFilter(workoutPlanFilter);
    PreparedQuery results = datastore.prepare(query);

    Entity workoutPlanEntity = results.asSingleEntity();

    // Updating Workout Plan with correct number of workout plan days user has completed
    Blob workoutPlanBlob = (Blob) workoutPlanEntity.getProperty("workoutPlan");
    WorkoutPlan workoutPlan = SerializationUtils.deserialize(workoutPlanBlob.getBytes());
    workoutPlan.setNumWorkoutDaysCompleted(numWorkoutDaysCompleted);
    byte[] workoutPlanData = SerializationUtils.serialize(workoutPlan);
    workoutPlanBlob = new Blob(workoutPlanData);

    // Updating entity and storing it back into datastore
    workoutPlanEntity.setProperty("userId", workoutPlan.getUserId());
    workoutPlanEntity.setProperty("workoutPlan", workoutPlanBlob);
    workoutPlanEntity.setProperty("workoutPlanId", workoutPlan.getWorkoutPlanId());
    workoutPlanEntity.setProperty("numWorkoutDaysCompleted", numWorkoutDaysCompleted);
    datastore.put(workoutPlanEntity);
  }

  /**
   * Returns number of WorkoutPlans created and saved in Datastore by current user (specified by
   * userId)
   *
   * @param userId The current logged-in user's ID number
   * @param datastore Datastore instance to retrieve WorkoutPlan information from database
   * @return number of workout plans created by userId + 1 to create unique id for new workout plan
   */
  public static int getWorkoutPlanId(String userId, DatastoreService datastore) {
    Filter userFilter = new FilterPredicate("userId", FilterOperator.EQUAL, userId);
    Query query = new Query("WorkoutPlan").setFilter(userFilter);
    PreparedQuery results = datastore.prepare(query);
    return Iterables.size(results.asIterable()) + 1;
  }

  /**
   * Retuns a composite filter for Query to retrieve WorkoutPlan Entity corresponding to userId and
   * workoutPlanId
   *
   * @param userId unique id of session to delete entities from
   * @param workoutPlanId unique id to retrieve correct entities
   * @return CompositeFilter with proper filters
   */
  public static Filter createWorkoutPlanFilter(String userId, int workoutPlanId) {
    return new CompositeFilter(
        CompositeFilterOperator.AND,
        Arrays.asList(
            new FilterPredicate("userId", FilterOperator.EQUAL, userId),
            new FilterPredicate("workoutPlanId", FilterOperator.EQUAL, workoutPlanId)));
  }

  /**
   * Stores all generated workout videos if user is logged in
   *
   * @param datastore Datastore instance to store workout video in database
   * @param workoutVideo YouTubeVideo object searched for by current user to store in database
   */
  public static void storeWorkoutVideo(DatastoreService datastore, YouTubeVideo workoutVideo) {

    // Check if video already stored to make sure each video is only stored once
    Filter workoutVideoFilter =
        createWorkoutVideoFilter(workoutVideo.getUserId(), workoutVideo.getVideoId());
    Query query = new Query("WorkoutVideo").setFilter(workoutVideoFilter);
    PreparedQuery results = datastore.prepare(query);

    if (results.asSingleEntity() == null) {
      // Transform YouTubeVideo to Blob to be able to store with Datastore
      byte[] workoutVideoData = SerializationUtils.serialize(workoutVideo);
      Blob workoutVideoBlob = new Blob(workoutVideoData);

      // Create WorkoutVideo Entity and store in Datastore
      Entity workoutVideoEntity = new Entity("WorkoutVideo");
      workoutVideoEntity.setProperty("userId", workoutVideo.getUserId());
      workoutVideoEntity.setProperty("workoutVideo", workoutVideoBlob);
      workoutVideoEntity.setProperty("workoutVideoId", workoutVideo.getVideoId());
      datastore.put(workoutVideoEntity);
    }
  }

  /**
   * Retrieves stored workout video (YouTubeVideo) that user wants to save
   *
   * @param userID The current logged-in user's ID number
   * @param workoutVideoId The id for the workout video they want to save
   * @param datastore Datastore instance to retrieve WorkoutVideo from database
   * @return YouTubeVideo workout video that user wants to save
   */
  public static YouTubeVideo getStoredWorkoutVideo(
      String userId, String workoutVideoId, DatastoreService datastore) {

    // Create Filter to retrieve WorkoutVideo entities based on userId and workoutVideoId
    Filter workoutVideoFilter = createWorkoutVideoFilter(userId, workoutVideoId);
    Query query = new Query("WorkoutVideo").setFilter(workoutVideoFilter);
    PreparedQuery results = datastore.prepare(query);

    Entity workoutVideoEntity = results.asSingleEntity();

    // Transform wokrout video Blob back into YouTubeVideo and return it
    Blob workoutVideoBlob = (Blob) workoutVideoEntity.getProperty("workoutVideo");
    YouTubeVideo workoutVideo = SerializationUtils.deserialize(workoutVideoBlob.getBytes());

    return workoutVideo;
  }

  /**
   * Saves YouTubeVideo workout video when requested by user (on button click)
   *
   * @param workoutVideo YouTubeVideo object current user wants to save in database
   * @param datastore Datastore instance to save workout video (YouTubeVideo) in database
   */
  public static void saveWorkoutVideo(YouTubeVideo workoutVideo, DatastoreService datastore) {

    // Check if video already saved to make sure each video is only saved once
    Filter workoutVideoFilter =
        createWorkoutVideoFilter(workoutVideo.getUserId(), workoutVideo.getVideoId());
    Query query = new Query("SavedWorkoutVideo").setFilter(workoutVideoFilter);
    PreparedQuery results = datastore.prepare(query);

    if (results.asSingleEntity() == null) {
      // Transform YouTubeVideo to Blob to be able to store with Datastore
      byte[] workoutVideoData = SerializationUtils.serialize(workoutVideo);
      Blob workoutVideoBlob = new Blob(workoutVideoData);

      // Create SavedWorkoutVideo Entity and store in Datastore
      Entity savedWorkoutVideoEntity = new Entity("SavedWorkoutVideo");
      savedWorkoutVideoEntity.setProperty("userId", workoutVideo.getUserId());
      savedWorkoutVideoEntity.setProperty("workoutVideo", workoutVideoBlob);
      savedWorkoutVideoEntity.setProperty("workoutVideoId", workoutVideo.getVideoId());
      datastore.put(savedWorkoutVideoEntity);
    }
  }

  /**
   * Retrieves saved workout videos (YouTubeVideo) to display on user's workout dashboard
   *
   * @param userID The current logged-in user's ID number
   * @param datastore Datastore instance to retrieve WorkoutPlans from database
   * @return ArrayList of YouTubeVideo objects that has user saved
   */
  public static ArrayList<YouTubeVideo> getSavedWorkoutVideos(
      String userId, DatastoreService datastore) {

    // Create Filter to retrieve SavedWorkoutVideo entities based on userId
    Filter savedWorkoutVideoFilter = new FilterPredicate("userId", FilterOperator.EQUAL, userId);
    Query query = new Query("SavedWorkoutVideo").setFilter(savedWorkoutVideoFilter);
    PreparedQuery results = datastore.prepare(query);

    ArrayList<YouTubeVideo> savedWorkoutVideos = new ArrayList<>();

    // For each saved SavedWorkoutVideo, transform YouTubeVideo Blob back into YouTubeVideo and add
    // to ArrayList
    for (Entity savedWorkoutVideoEntity : results.asIterable()) {
      Blob workoutVideoBlob = (Blob) savedWorkoutVideoEntity.getProperty("workoutVideo");
      YouTubeVideo workoutVideo = SerializationUtils.deserialize(workoutVideoBlob.getBytes());
      savedWorkoutVideos.add(workoutVideo);
    }

    return savedWorkoutVideos;
  }

  /**
   * Retuns a composite filter for Query to retrieve WorkoutVideo Entity corresponding to userId and
   * workoutVideoId
   *
   * @param userId unique id of session to delete entities from
   * @param workoutVideoId unique id to retrieve correct entities
   * @return CompositeFilter with proper filters
   */
  public static Filter createWorkoutVideoFilter(String userId, String workoutVideoId) {
    return new CompositeFilter(
        CompositeFilterOperator.AND,
        Arrays.asList(
            new FilterPredicate("userId", FilterOperator.EQUAL, userId),
            new FilterPredicate("workoutVideoId", FilterOperator.EQUAL, workoutVideoId)));
  }
}
