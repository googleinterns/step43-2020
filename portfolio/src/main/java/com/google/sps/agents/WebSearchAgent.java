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

package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.protobuf.Value;
import java.util.Map;

/**
 * WebSearch Agent handles users' requests for time information. It determines appropriate outputs
 * and display information to send to the user interface based on Dialogflow's detected WebSearch
 * intents.
 */
public class WebSearchAgent implements Agent {
  private final String intentName;
  private String searchText;

  /**
   * Web Search agent constructor that uses intent and parameter to determnine fulfillment for user
   * request.
   *
   * @param intentName String containing the specific intent within memory agent that user is
   *     requesting.
   * @param parameters Map containing the detected entities in the user's intent.
   */
  public WebSearchAgent(String intentName, Map<String, Value> parameters) {
    this.intentName = intentName;
    setParameters(parameters);
  }

  /**
   * Method that handles parameter assignment for fulfillment text and display based on the user's
   * input intent and extracted parameters
   *
   * @param parameters Map containing the detected entities in the user's intent.
   */
  public void setParameters(Map<String, Value> parameters) {
    searchText = parameters.get("q").getStringValue();
  }

  @Override
  public String getOutput() {
    return "Redirecting to Google Search for " + searchText;
  }

  @Override
  public String getDisplay() {
    return null;
  }

  @Override
  public String getRedirect() {
    String baseURL = "http://www.google.com/search?q=";
    String[] individualWords = searchText.split(" ");
    String endURL = String.join("+", individualWords);
    return baseURL + endURL;
  }
}
