/*
 * Copyright 2018 Google Inc.
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

package com.google.sps.data;

// Imports the Google Cloud client library
import com.google.api.services.books.*;
import com.google.gson.*;
import com.google.protobuf.Value;
import com.google.sps.utils.AgentUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

/**
 * A BookQuery object contains parameters neccessary to make a call to the Google Books API that
 * will match users' requests to Dialogflow
 *
 * <p>A BookQuery object is only created by createBookQuery() function, ensuring that any BookQuery
 * object is only created with valid parameters. Specifically, it ensures that all BookQuery objects
 * have a valid userInput property, since this is the only field required to make a request to the
 * Google Books API.
 */
public class BookQuery implements Serializable {

  private String userInput;
  private String intent;
  private String type;
  private String categories;
  private String authors;
  private String title;
  private String order;
  private String language;
  private String bookshelfName;
  private String queryString;
  private boolean isMyLibrary;
  private String friendName;

  /**
   * Creates a BookQuery object for the detected parameters from Dialogflow that will be used to
   * retrieve appropriate Volumes from the Google Books API that match a user input for user
   * requests that do not require user Authentication, or throws an exception if the userInput is
   * empty
   *
   * @param intent detected intent from Dialogflow
   * @param userInput string
   * @param parameters parameter Map from Dialogflow
   * @return BookQuery object
   */
  public static BookQuery createBookQuery(
      String intent, String userInput, Map<String, Value> parameters)
      throws IllegalArgumentException {
    return createBookQuery(intent, userInput, parameters, false);
  }

  /**
   * Creates a BookQuery object for the detected parameters from Dialogflow that will be used to
   * retrieve appropriate Volumes from the Google Books API that match a user input, or throws an
   * exception if the userInput is empty
   *
   * @param intent detected intent from Dialogflow
   * @param String userInput
   * @param parameters parameter Map from Dialogflow
   * @param requiresAuth determines whether request requires user authentication
   * @return BookQuery object
   */
  public static BookQuery createBookQuery(
      String intent, String userInput, Map<String, Value> parameters, Boolean requiresAuth)
      throws IllegalArgumentException {
    if (userInput == null || userInput.isEmpty()) {
      throw new IllegalArgumentException();
    } else {
      BookQuery bookQuery = new BookQuery(intent, userInput, parameters, requiresAuth);
      return bookQuery;
    }
  }

  /**
   * Private BookQuery constructor, can only be called by createBookQuery() if user input string is
   * valid.
   *
   * <p>If Dialogflow does not detect certain parameters for a user request, then BookQuery
   * properties will remain null
   *
   * @param intent detected intent from Dialogflow
   * @param userInput detected input string
   * @param parameters parameter Map from Dialogflow
   * @param requiresAuth boolean determining if intent requires auth
   */
  private BookQuery(
      String intent, String userInput, Map<String, Value> parameters, Boolean requiresAuth) {
    this.intent = intent;
    this.userInput = userInput;
    this.isMyLibrary = requiresAuth;
    setType(parameters.get("type"));
    setCategories(parameters.get("categories"));
    setAuthors(parameters.get("authors"));
    setTitle(parameters.get("title"));
    setOrder(parameters.get("order"));
    setLanguage(parameters.get("language"));
    setBookshelf(parameters.get("bookshelf"));
    setFriendName(parameters.get("friend"));
    setQueryString();
  }

  private void setType(Value paramValue) {
    if (paramValue != null && !paramValue.getStringValue().isEmpty()) {
      this.type = paramValue.getStringValue();
    }
  }

  private void setCategories(Value paramValue) {
    if (paramValue != null && !paramValue.getStringValue().isEmpty()) {
      this.categories = paramValue.getStringValue();
    }
  }

  private void setAuthors(Value paramValue) {
    if (paramValue != null) {
      ArrayList<Value> valueList = new ArrayList<Value>(paramValue.getListValue().getValuesList());
      ArrayList<String> authorList = new ArrayList<String>();
      for (int i = 0; i < valueList.size(); ++i) {
        if (getValidName(valueList.get(i)) != null) {
          String authorName = String.join("+", getValidName(valueList.get(i)).split(" "));
          authorList.add("inauthor:\"" + authorName + "\"");
        }
      }
      if (!authorList.isEmpty()) {
        this.authors = String.join("+", authorList);
      }
    }
  }

  private void setTitle(Value paramValue) {
    if (paramValue != null && !paramValue.getStringValue().isEmpty()) {
      this.title = "intitle:\"" + String.join("+", paramValue.getStringValue().split(" ")) + "\"";
    }
  }

  private void setOrder(Value paramValue) {
    if (paramValue != null && !paramValue.getStringValue().isEmpty()) {
      this.order = paramValue.getStringValue();
    }
  }

  private void setLanguage(Value paramValue) {
    if (paramValue != null && !paramValue.getStringValue().isEmpty()) {
      String languageName = paramValue.getStringValue();
      this.language = AgentUtils.getLanguageCode(languageName);
    }
  }

  private void setQueryString() {
    String queryInput = this.userInput;
    if (this.userInput.toLowerCase().startsWith("show me ")) {
      queryInput = userInput.substring(8);
    }
    String queryText = String.join("+", queryInput.split(" "));
    if (this.authors != null) {
      queryText += "+" + this.authors;
    }
    if (this.title != null) {
      queryText += "+" + this.title;
    }
    this.queryString = queryText;
  }

  private void setBookshelf(Value paramValue) {
    if (paramValue != null && !paramValue.getStringValue().isEmpty()) {
      String name = paramValue.getStringValue();
      this.bookshelfName = name.substring(0, 1).toUpperCase() + name.substring(1);
    }
  }

  private void setFriendName(Value paramValue) {
    if (getValidName(paramValue) != null) {
      String friend = getValidName(paramValue);
      if (friend.endsWith("'s")) {
        friend = friend.substring(0, friend.length() - 2);
      }
      String[] names = friend.split(" ");
      for (int i = 0; i < names.length; ++i) {
        if (names[i].length() > 1) {
          names[i] = names[i].substring(0, 1).toUpperCase() + names[i].substring(1).toLowerCase();
        }
      }
      this.friendName = String.join(" ", names);
    }
  }

  public String getIntent() {
    return this.intent;
  }

  public String getBookshelfName() {
    return this.bookshelfName;
  }

  public String getTitle() {
    return this.title;
  }

  public String getAuthors() {
    return this.authors;
  }

  public String getType() {
    return this.type;
  }

  public String getCategories() {
    return this.categories;
  }

  public String getOrder() {
    return this.order;
  }

  public String getLanguage() {
    return this.language;
  }

  public String getUserInput() {
    return this.userInput;
  }

  public String getFriendName() {
    return this.friendName;
  }

  public String getQueryString() {
    return this.queryString;
  }

  public Boolean isMyLibrary() {
    return this.isMyLibrary;
  }

  /**
   * Returns the name parameter if the paramValue contains a valid, non-empty "name" parameter,
   * based on Dialogflow's person parameter format for a single person. Example: {"name": "abby
   * mapes"} returns "abby mapes" if Map has a non-empty name parameter. Otherwise, returns null
   *
   * @param paramValue single value Map
   * @return validName string
   */
  private String getValidName(Value paramValue) {
    if (paramValue != null
        && paramValue.getStructValue() != null
        && paramValue.getStructValue().getFieldsMap() != null
        && paramValue.getStructValue().getFieldsMap().get("name") != null
        && !paramValue.getStructValue().getFieldsMap().get("name").getStringValue().isEmpty()) {
      return paramValue.getStructValue().getFieldsMap().get("name").getStringValue();
    }
    return null;
  }
}
