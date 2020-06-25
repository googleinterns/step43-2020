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

import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.sps.utils.AgentUtils;
import com.google.sps.data.Output;
import com.google.sps.utils.SpeechUtils;
import com.google.sps.utils.TextUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.util.stream.Collectors;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
/** Servlet that takes in user text input and retrieves 
 ** QueryResult from Dialogflow input string to display. */
 
@WebServlet("/text-input")
public class TextInputServlet extends HttpServlet {
 
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
 
    String userQuestion = request.getParameter("request-input");
    String language = request.getParameter("language");
    String languageCode = AgentUtils.getLanguageCode(language);
    QueryResult result = TextUtils.detectIntentStream(userQuestion, languageCode);
 
    if (result == null) {
      response.getWriter().write(new Gson().toJson(null));
      return;
    }

    Output output = null;
    try{
        output = AgentUtils.getOutput(result, languageCode);
    }catch (Exception e) {
        e.printStackTrace();
    }

    //Convert to JSON string
    String json = new Gson().toJson(output);
    response.getWriter().write(json);
  }
}
