// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.gson.Gson;
import com.google.sps.data.DialogFlowClient;
import com.google.sps.data.Output;
import com.google.sps.utils.AgentUtils;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet that takes in user text input and retrieves * QueryResult from Dialogflow input string to
 * display.
 */
@WebServlet("/text-input")
public class TextInputServlet extends HttpServlet {

  private static Logger log = LoggerFactory.getLogger(TextInputServlet.class);
  private DatastoreService datastore = createDatastore();
  private UserService userService = createUserService();

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    String userQuestion = request.getParameter("request-input");
    String language = request.getParameter("language");
    String languageCode = AgentUtils.getLanguageCode(language);
    DialogFlowClient result = detectIntentStream(userQuestion, languageCode);

    if (result == null) {
      response.getWriter().write(new Gson().toJson(null));
      return;
    }
    Output output = null;
    try {
      output = AgentUtils.getOutput(result, languageCode, userService, datastore);
    } catch (Exception e) {
      e.printStackTrace();
    }
    // Convert to JSON string
    String json = new Gson().toJson(output);
    response.getWriter().write(json);
  }

  public DialogFlowClient detectIntentStream(String text, String languageCode) {
    DialogFlowClient dialogFlowResult = null;

    try (SessionsClient sessionsClient = SessionsClient.create()) {
      dialogFlowResult = createDialogFlow(text, languageCode, sessionsClient);

      log.info("====================");
      log.info("Query Text: '" + dialogFlowResult.getQueryText() + "'\n");
      log.info(
          "Detected Intent: "
              + dialogFlowResult.getIntentName()
              + " (confidence: "
              + dialogFlowResult.getIntentConfidence()
              + ")\n");
      log.info("Fulfillment Text: '" + dialogFlowResult.getFulfillmentText() + "'\n");

    } catch (IOException e) {
      e.printStackTrace();
    }
    return dialogFlowResult;
  }

  protected DialogFlowClient createDialogFlow(
      String text, String languageCode, SessionsClient sessionsClient) {
    return new DialogFlowClient(text, languageCode, sessionsClient);
  }

  protected UserService createUserService() {
    return UserServiceFactory.getUserService();
  }

  protected DatastoreService createDatastore() {
    return DatastoreServiceFactory.getDatastoreService();
  }
}
