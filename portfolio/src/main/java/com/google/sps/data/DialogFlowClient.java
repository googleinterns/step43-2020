package com.google.sps.data;

import com.google.api.gax.rpc.BidiStream;
import com.google.cloud.dialogflow.v2.AudioEncoding;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.InputAudioConfig;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentRequest;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentResponse;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.protobuf.ByteString;
import com.google.protobuf.Value;
import java.util.Map;

public class DialogFlowClient {

  static SessionName session = SessionName.of("mihira-step-2020-3", "1");
  QueryResult queryResult;

  // Constructor for text inputs
  public DialogFlowClient(String text, String languageCode, SessionsClient sessionsClient) {
    TextInput.Builder textInput =
        TextInput.newBuilder().setText(text).setLanguageCode(languageCode);
    QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
    DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);
    queryResult = response.getQueryResult();
  }

  // Constructor for audio inputs
  public DialogFlowClient(
      SessionsClient sessionsClient, ByteString audioBytestring, int sampleRate) {
    InputAudioConfig inputAudioConfig =
        InputAudioConfig.newBuilder()
            .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_LINEAR_16)
            .setLanguageCode("en-US")
            .setSampleRateHertz(sampleRate)
            .build();
    QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();

    BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream =
        makeBidiStream(sessionsClient, queryInput, audioBytestring);
    for (StreamingDetectIntentResponse response : bidiStream) {
      queryResult = response.getQueryResult();
    }
  }

  private static BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse>
      makeBidiStream(
          SessionsClient sessionsClient, QueryInput queryInput, ByteString audioBytestring) {
    BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream =
        sessionsClient.streamingDetectIntentCallable().call();
    bidiStream.send(
        StreamingDetectIntentRequest.newBuilder()
            .setSession(session.toString())
            .setQueryInput(queryInput)
            .build());
    bidiStream.send(
        StreamingDetectIntentRequest.newBuilder().setInputAudio(audioBytestring).build());
    bidiStream.closeSend();
    return bidiStream;
  }

  public String getQueryText() {
    return queryResult.getQueryText();
  }

  public String getIntentName() {
    return queryResult.getIntent().getDisplayName();
  }

  public float getIntentConfidence() {
    return queryResult.getIntentDetectionConfidence();
  }

  public String getFulfillmentText() {
    return queryResult.getFulfillmentText();
  }

  public Map<String, Value> getParameters() {
    return queryResult.getParameters().getFieldsMap();
  }

  public boolean getAllRequiredParamsPresent() {
    return queryResult.getAllRequiredParamsPresent();
  }
}
