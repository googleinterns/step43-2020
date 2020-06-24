package com.google.sps.utils;
 
// Imports the Google Cloud client library
import com.google.api.gax.rpc.BidiStream;
import com.google.cloud.dialogflow.v2.AudioEncoding;
import com.google.cloud.dialogflow.v2.InputAudioConfig;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentRequest;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeRequest;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 
/**
 * DialogFlow API Detect Intent sample with audio files processes as an audio stream.
 */
public class AudioUtils {
 
  static SessionName session = SessionName.of("mihira-step-2020-3", "1");
 
  public static QueryResult detectIntentStream(ByteString bytestring) {
    QueryResult queryResult = null;
 
    try (SessionsClient sessionsClient = SessionsClient.create()) {
      InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
          .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_LINEAR_16)
          .setLanguageCode("en-US")
          .setSampleRateHertz(48000)
          .build();
      QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();
 
      BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream = 
        makeBidiStream(sessionsClient, queryInput, bytestring);
 
      for (StreamingDetectIntentResponse response : bidiStream) {
        queryResult = response.getQueryResult();
        printResult(queryResult);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return queryResult;
  }

/**
 * Transcribe a short audio file using synchronous speech recognition
 *
 * @param localFilePath Path to local audio file, e.g. /path/audio.wav
 */
public static String detectSpeechLanguage(byte[] data, String languageCode) {
  try (SpeechClient speechClient = SpeechClient.create()) {
    int sampleRateHertz = 48000;

    RecognitionConfig.AudioEncoding encoding = RecognitionConfig.AudioEncoding.LINEAR16;
    RecognitionConfig config =
        RecognitionConfig.newBuilder()
            .setLanguageCode(languageCode)
            .setSampleRateHertz(sampleRateHertz)
            .setEncoding(encoding)
            .build();
    ByteString content = ByteString.copyFrom(data);
    RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(content).build();
    RecognizeRequest request =
        RecognizeRequest.newBuilder().setConfig(config).setAudio(audio).build();
    RecognizeResponse response = speechClient.recognize(request);
    for (SpeechRecognitionResult result : response.getResultsList()) {
      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
      System.out.printf("Transcript: %s\n", alternative.getTranscript());
      return alternative.getTranscript();
    }
  } catch (Exception exception) {
    System.err.println("Failed to create the client due to: " + exception);
  }
  return null;
}

  public static void detectIntentStream(String projectId, String audioFilePath, String sessionId) {
    try (SessionsClient sessionsClient = SessionsClient.create()) {
      InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
          .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_LINEAR_16)
          .setLanguageCode("en-US") // languageCode = "en-US"
          .setSampleRateHertz(16000) // sampleRateHertz = 16000
          .build();
      QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();
 
      BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream =
          sessionsClient.streamingDetectIntentCallable().call();

      bidiStream.send(StreamingDetectIntentRequest.newBuilder()
          .setSession(session.toString())
          .setQueryInput(queryInput)
          .build());

      try (FileInputStream audioStream = new FileInputStream(audioFilePath)) {
        byte[] buffer = new byte[4096];
        int bytes;
        while ((bytes = audioStream.read(buffer)) != -1) {
          bidiStream.send(
              StreamingDetectIntentRequest.newBuilder()
                  .setInputAudio(ByteString.copyFrom(buffer, 0, bytes))
                  .build());
        }
      }
      bidiStream.closeSend();

      for (StreamingDetectIntentResponse response : bidiStream) {
        QueryResult queryResult = response.getQueryResult();
        printResult(queryResult);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> makeBidiStream(SessionsClient sessionsClient, QueryInput queryInput, ByteString bytestring) {
    BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream =
          sessionsClient.streamingDetectIntentCallable().call();
      bidiStream.send(StreamingDetectIntentRequest.newBuilder()
          .setSession(session.toString())
          .setQueryInput(queryInput)
          .build());
      bidiStream.send(
          StreamingDetectIntentRequest.newBuilder()
              .setInputAudio(bytestring)
              .build());
      bidiStream.closeSend();
      return bidiStream;
  }

  private static void printResult(QueryResult queryResult) {
    System.out.println("====================");
    System.out.format("Intent Display Name: %s\n", queryResult.getIntent().getDisplayName());
    System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
    System.out.format("Detected Intent: %s (confidence: %f)\n",
      queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
    System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());
  }
}
