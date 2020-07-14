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
import com.google.cloud.translate.*;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.sps.agents.TranslateAgent;
import com.google.sps.data.DialogFlowClient;
import com.google.sps.data.Output;
import com.google.sps.utils.AgentUtils;
import com.google.sps.utils.AudioUtils;
import com.google.sps.utils.SpeechUtils;
import java.io.IOException;
import java.util.*;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that takes in audio stream and retrieves * user input string to display. */
@WebServlet("/audio-input")
public class AudioInputServlet extends HttpServlet {

  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private UserService userService = UserServiceFactory.getUserService();

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    // Convert input stream into bytestring for DialogFlow API input
    ServletInputStream stream = request.getInputStream();
    ByteString bytestring = ByteString.readFrom(stream);
    String language = request.getParameter("language");
    Output output = null;

    if (language.equals("English")) {
      output = handleEnglishQuery(bytestring, null);
    } else {
      try {
        output = handleForeignQuery(bytestring, language);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    // Convert to JSON string
    String json = new Gson().toJson(output);
    response.getWriter().write(json);
  }

  private Output handleEnglishQuery(ByteString bytestring, String languageCode) {
    DialogFlowClient result = AudioUtils.detectIntentStream(bytestring);
    if (result == null) {
      return null;
    }
    return AgentUtils.getOutput(result, languageCode, userService, datastore);
  }

  private Output handleForeignQuery(ByteString bytestring, String language) {
    String languageCode = AgentUtils.getLanguageCode(language);
    String detectedUserInputString =
        AudioUtils.detectSpeechLanguage(bytestring.toByteArray(), languageCode);
    String englishLanguageCode = AgentUtils.getLanguageCode("English");
    // Google Translate API - convert detectedUserInputString from language to English
    Translation inputTranslation =
        TranslateAgent.translate(detectedUserInputString, languageCode, englishLanguageCode);

    String translatedInputText = inputTranslation.getTranslatedText();
    ByteString inputByteString = null;
    try {
      inputByteString = SpeechUtils.synthesizeText(translatedInputText, languageCode);
    } catch (Exception e) {
      e.printStackTrace();
    }

    DialogFlowClient englishOutput =
        (new TextInputServlet()).detectIntentStream(translatedInputText, englishLanguageCode);

    // Google Translate API - convert input and fulfillment to appropriate language
    String userInput = englishOutput.getQueryText();
    String fulfillment = englishOutput.getFulfillmentText();
    String userInputTranslation =
        TranslateAgent.translate(userInput, englishLanguageCode, languageCode).getTranslatedText();
    String fulfillmentTranslation =
        TranslateAgent.translate(fulfillment, englishLanguageCode, languageCode)
            .getTranslatedText();
    byte[] byteArray = AgentUtils.getByteStringToByteArray(fulfillmentTranslation, languageCode);
    Output languageOutput =
        new Output(
            userInputTranslation, fulfillmentTranslation, byteArray, englishOutput.getIntentName());
    return languageOutput;
  }
}
