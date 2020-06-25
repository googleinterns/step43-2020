package com.google.sps.utils;

import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;
import java.io.IOException;

/**
 * DialogFlow API Detects Intent with input text.
**/

public class TextUtils {

  static SessionName session = SessionName.of("mihira-step-2020-3", "1");

  public static QueryResult detectIntentStream(String text, String languageCode) {
    QueryResult queryResult = null;

    try (SessionsClient sessionsClient = SessionsClient.create()) {
        TextInput.Builder textInput =
            TextInput.newBuilder().setText(text).setLanguageCode(languageCode);

        // Build the query with the TextInput
        QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

        // Performs the detect intent request
        DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

        // Display the query result
        queryResult = response.getQueryResult();

        System.out.println("====================");
        System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
        System.out.format("Detected Intent: %s (confidence: %f)\n",
            queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
        System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());

    } catch (IOException e) {
      e.printStackTrace();
    }
    return queryResult;
  }
}
