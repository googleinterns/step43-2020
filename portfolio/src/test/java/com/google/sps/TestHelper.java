package com.google.sps.servlets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.Struct.Builder;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.sps.data.DialogFlowClient;
import com.google.sps.data.Output;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class TestHelper {

  @Mock DialogFlowClient dialogFlowMock;

  @InjectMocks TextInputServlet textInputServlet;

  HttpServletRequest request;
  HttpServletResponse response;
  TextInputServlet servlet;

  // Constructor for test that uses the real DialogFlow
  // Use only for testing DialogFlow's response to training data or integration testing
  public TestHelper(String inputText) {
    request = mock(HttpServletRequest.class);
    when(request.getParameter("request-input")).thenReturn(inputText);
    response = mock(HttpServletResponse.class);
    servlet = new TextInputServlet();
  }

  // Constructor for specific Agent unit tests
  // DialogFlow is fully mocked in this version based on the parameters passed in.
  public TestHelper(String inputText, String parameters, String intentName)
      throws InvalidProtocolBufferException {
    this(inputText);
    servlet = new TestableTextInputServlet();
    dialogFlowMock = mock(DialogFlowClient.class);
    Map<String, Value> map = stringToMap(parameters);
    when(dialogFlowMock.getParameters()).thenReturn(map);
    when(dialogFlowMock.getQueryText()).thenReturn(inputText);
    when(dialogFlowMock.getIntentName()).thenReturn(intentName);
    when(dialogFlowMock.getIntentConfidence()).thenReturn((float) 1.0);
    when(dialogFlowMock.getFulfillmentText()).thenReturn("");
    when(dialogFlowMock.getAllRequiredParamsPresent()).thenReturn(true);
  }

  // Retrieves output in the same form as that which is passed to javascript
  public Output getOutput() throws Exception {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);

    servlet.doPost(request, response);

    verify(request, atLeast(1)).getParameter("request-input");
    writer.flush();
    Output output = new Gson().fromJson(stringWriter.toString(), Output.class);
    return output;
  }

  private class TestableTextInputServlet extends TextInputServlet {
    @Override
    public DialogFlowClient createDialogFlow(
        String text, String languageCode, SessionsClient sessionsClient) {
      return dialogFlowMock;
    }
  }

  public static Map<String, Value> stringToMap(String json) throws InvalidProtocolBufferException {
    JSONObject jsonObject = new JSONObject(json);
    Builder structBuilder = Struct.newBuilder();
    JsonFormat.parser().merge(jsonObject.toString(), structBuilder);
    Struct struct = structBuilder.build();
    return struct.getFieldsMap();
  }
}
