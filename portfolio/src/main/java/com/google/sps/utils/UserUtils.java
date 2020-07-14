package com.google.sps.utils;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.users.UserService;
import com.google.sps.data.Pair;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserUtils {

  private static Logger log = LoggerFactory.getLogger(UserUtils.class);

  /**
   * Returns a string containing the name we should use to address the user. The name returned is
   * determined by info the user has provided in order of: 1. nickname 2. first name (if no
   * nickname) 3. email (if no name is provided) This method assumes that the user is logged in.
   *
   * @return a string containing the name the conversational AI will use to address the user
   */
  public static String getDisplayName(UserService userService, DatastoreService datastore) {
    try {
      Entity entity =
          datastore.get(KeyFactory.createKey("UserInfo", userService.getCurrentUser().getUserId()));
      return getProperName(userService, entity);
    } catch (EntityNotFoundException e) {
      return userService.getCurrentUser().getEmail();
    }
  }

  private static String getProperName(UserService userService, Entity entity) {
    String name = (String) entity.getProperty("nickname");
    if (name == null) {
      name = (String) entity.getProperty("first name");
    }
    if (name == null) {
      name = userService.getCurrentUser().getEmail();
    }
    return name;
  }

  /**
   * Saves name information into user database. This method assumes that the user is logged in or
   * else it will throw an IllegalStateException error.
   *
   * @param nameType String that specifies which part of user's name the given name refers to:
   *     "first name", "middle name", or "last name"
   * @param name String containing the name to be saved
   */
  public static void saveName(
      String userID, DatastoreService datastore, String nameType, String name) {
    Entity entity;
    try {
      entity = datastore.get(KeyFactory.createKey("UserInfo", userID));
    } catch (Exception e) {
      entity = new Entity("UserInfo", userID);
      entity.setProperty("id", userID);
    }
    entity.setProperty(nameType, name);
    datastore.put(entity);
  }

  /**
   * Saves comment information into comment history database if the user is logged in.
   *
   * @param userComment The comment written by the user.
   * @param assistantComment The fulfillment comment returned by the assistant.
   */
  public static void saveComment(
      String userID, DatastoreService datastore, String userComment, String assistantComment) {
    if (userID != null) {
      makeCommentEntity(userID, datastore, userComment, true);
      makeCommentEntity(userID, datastore, assistantComment, false);
    }
  }

  /**
   * Creates a comment entity and stores it in the given user's database
   *
   * @param userID The ID corresponding to user who made the comment
   * @param datastore Database instance.
   * @param comment String comment to be stored.
   * @param isUser Boolean indicating whether the comment was said by user or assistant.
   */
  public static void makeCommentEntity(
      String userID, DatastoreService datastore, String comment, boolean isUser) {
    Entity entity = new Entity("CommentHistory");
    entity.setProperty("id", userID);
    entity.setProperty("isUser", isUser);
    entity.setProperty("comment", comment);
    entity.setProperty("timestamp", System.currentTimeMillis());
    datastore.put(entity);
  }

  /**
   * Retrieves a list of (Entity, List<Entity>)-pairs where the Entity (pair key) is a datastore
   * entity with a comment containing the desired keyword and the List<Entity> (pair value) is a
   * list of datastore entities containing comments that are around the chosen Entity.
   *
   * @param userID The logged-in user's ID
   * @param keyword The fulfillment comment returned by the assistant.
   * @return List of pairs where key corresponds to identified entity with keyword and value is a
   *     list of surrounding entities
   */
  public static List<Pair<Entity, List<Entity>>> getKeywordCommentEntities(
      DatastoreService datastore, String userID, String keyword) {
    Filter currentUserFilter = new FilterPredicate("id", FilterOperator.EQUAL, userID);
    Query query =
        new Query("CommentHistory")
            .setFilter(currentUserFilter)
            .addSort("timestamp", SortDirection.ASCENDING);

    List<Pair<Entity, List<Entity>>> keywordEntities = new ArrayList<>();
    List<Entity> results = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
    for (int i = 0; i < results.size(); i++) {
      Entity entity = results.get(i);
      String comment = (String) entity.getProperty("comment");
      if (comment.toLowerCase().contains(keyword)) {
        keywordEntities.add(new Pair(entity, getSurroundingConversation(results, i)));
      }
    }
    return keywordEntities;
  }

  private static List<Entity> getSurroundingConversation(List<Entity> results, int index) {
    List<Entity> surroundingEntities = new ArrayList<>();
    for (int i = Math.max(index - 6, 0); i < Math.min(index + 7, results.size()); i++) {
      surroundingEntities.add(results.get(i));
    }
    return surroundingEntities;
  }
}
