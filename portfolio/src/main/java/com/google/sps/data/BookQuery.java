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
import com.google.protobuf.Struct;
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
  private String type;
  private String categories;
  private String authors;
  private String title;
  private String order;
  private String language;
  private String bookshelfName;
  private String queryString;

  /**
   * Creates a BookQuery object the detected parameters from Dialogflow that will be used to
   * retrieve appropriate Volumes from the Google Books API that match a user input, or throws an
   * exception if the userInput is empty
   *
   * @param String userInput
   * @param parameters parameter Map from Dialogflow
   * @return BookQuery object
   */
  public static BookQuery createBookQuery(String userInput, Map<String, Value> parameters)
      throws IllegalArgumentException {
    if (userInput == null || userInput.isEmpty()) {
      throw new IllegalArgumentException();
    } else {
      BookQuery bookQuery = new BookQuery(userInput, parameters);
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
   * @param userInput detected input string
   * @param parameters parameter Map from Dialogflow
   */
  private BookQuery(String userInput, Map<String, Value> parameters) {
    this.userInput = userInput;
    setType(parameters.get("type"));
    setCategories(parameters.get("categories"));
    setAuthors(parameters.get("authors"));
    setTitle(parameters.get("title"));
    setOrder(parameters.get("order"));
    setLanguage(parameters.get("language"));
    setBookshelf(parameters.get("bookshelf"));
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
        Struct personStruct = valueList.get(i).getStructValue();
        Map<String, Value> personFields = personStruct.getFieldsMap();
        if (personFields.get("name") != null
            && !personFields.get("name").getStringValue().isEmpty()) {
          String authorName =
              String.join("+", personFields.get("name").getStringValue().split(" "));
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

  public String getQueryString() {
    return this.queryString;
  }
}
