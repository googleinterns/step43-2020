package com.google.sps.servlets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.Struct.Builder;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.sps.data.DialogFlowClient;
import com.google.sps.data.Output;
import com.google.sps.utils.UserUtils;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHelper {

  private static Logger log = LoggerFactory.getLogger(TestHelper.class);

  @Mock DialogFlowClient dialogFlowMock;
  @Mock UserService userServiceMock;

  @InjectMocks TextInputServlet textInputServlet;

  HttpServletRequest request;
  HttpServletResponse response;
  TextInputServlet servlet;
  DatastoreService customDatastore;

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  /** Default constructor for TestHelper that sets up testing environment and empty mocks. */
  public TestHelper() {
    helper.setUp();
    customDatastore = DatastoreServiceFactory.getDatastoreService();
    userServiceMock = mock(UserService.class);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    dialogFlowMock = mock(DialogFlowClient.class);
    servlet = new TestableTextInputServlet();
    setLoggedIn();
  }

  /**
   * TestHelper constructor that runs end-to-end testing of Dialogflow and agents. Use to test both
   * Dialogflow's ability to detect the proper intent and verify agent fulfillment. Default has a
   * logged in user. (Must set logged out user manually using set method).
   *
   * @param inputText Text input of user's intent sent to Dialogflow.
   */
  public TestHelper(String inputText) {
    this();
    servlet = new TestableDFTextInputServlet();
    setInputText(inputText);
  }

  /**
   * TestHelper constructor that mocks Dialogflow with expected intent detection. Use to test proper
   * agent fulfillment given the correct Dialogflow output.
   *
   * @param inputText Text input of user's intent sent to Dialogflow.
   * @param parameters String json that consists of expected parameters identified by Dialogflow.
   * @param intentName String name of expected intent identified by Dialogflow.
   */
  public TestHelper(String inputText, String parameters, String intentName)
      throws InvalidProtocolBufferException {
    this();
    servlet = new TestableTextInputServlet();
    setParameters(inputText, parameters, intentName);
  }

  /**
   * TestHelper constructor that mocks Dialogflow in case of insufficient intent detection. Use to
   * test proper agent fulfillment given incomplete required parameters.
   *
   * @param inputText Text input of user's intent sent to Dialogflow.
   * @param parameters String json that consists of expected parameters identified by Dialogflow.
   * @param intentName String name of expected intent identified by Dialogflow.
   * @param allParamsPresent Boolean indicating if all required parameters are present.
   */
  public TestHelper(
      String inputText, String parameters, String intentName, Boolean allParamsPresent)
      throws InvalidProtocolBufferException {
    this(inputText, parameters, intentName);
    setParamsPresent(allParamsPresent);
  }

  /**
   * Gets the output object created by agent fulfillment after at the end of back-end process.
   *
   * @return Output object identical to that which is passed back to javascript.
   */
  public Output getOutput() throws IOException {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);

    servlet.doPost(request, response);

    verify(request, atLeast(1)).getParameter("request-input");
    writer.flush();
    Output output = new Gson().fromJson(stringWriter.toString(), Output.class);
    return output;
  }

  /**
   * Sets the input text to mock http request.
   *
   * @param inputText Text input of user's intent sent to Dialogflow.
   */
  public void setInputText(String inputText) {
    when(request.getParameter("request-input")).thenReturn(inputText);
  }

  /**
   * Sets the general return parameters to mock dialogflow.
   *
   * @param inputText Text input of user's intent sent to Dialogflow.
   * @param parameters String json that consists of expected parameters identified by Dialogflow.
   * @param intentName String name of expected intent identified by Dialogflow.
   */
  public void setParameters(String inputText, String parameters, String intentName)
      throws InvalidProtocolBufferException {
    setInputText(inputText);
    Map<String, Value> map = stringToMap(parameters);
    when(dialogFlowMock.getParameters()).thenReturn(map);
    when(dialogFlowMock.getIntentName()).thenReturn(intentName);
    when(dialogFlowMock.getQueryText()).thenReturn(inputText);
    when(dialogFlowMock.getIntentConfidence()).thenReturn((float) 1.0);
    when(dialogFlowMock.getFulfillmentText()).thenReturn("");
    when(dialogFlowMock.getAllRequiredParamsPresent()).thenReturn(true);
  }

  /**
   * Sets the mock returned for dialogflow all parameters present.
   *
   * @param allParamsPresent Boolean indicating if all required parameters are present.
   */
  public void setParamsPresent(boolean allParamsPresent) {
    when(dialogFlowMock.getAllRequiredParamsPresent()).thenReturn(allParamsPresent);
  }

  /**
   * Sets the mocks returned for a default logged-in user. Default user has: email:
   * "test@example.com" user id: "1"
   */
  public void setLoggedIn() {
    setUser("test@example.com", "1");
  }

  /** Sets the user service mock to return a logged-out user. */
  public void setLoggedOut() {
    when(userServiceMock.isUserLoggedIn()).thenReturn(false);
  }

  /**
   * Creates a customized user.
   *
   * @param email String containing the user's email.
   * @param id String containing the user's id number.
   */
  public void setUser(String email, String id) {
    when(userServiceMock.isUserLoggedIn()).thenReturn(true);
    when(userServiceMock.getCurrentUser()).thenReturn(new User(email, "authDomain", id));
  }

  /**
   * Populates customizes datastore with desired string comments.
   *
   * @param comments List of strings containing comments to be stored in custom database.
   */
  public void setCustomDatabase(List<String> comments) {
    for (String comment : comments) {
      UserUtils.makeCommentEntity("1", customDatastore, comment, true);
    }
  }

  private class TestableTextInputServlet extends TextInputServlet {
    @Override
    public DialogFlowClient createDialogFlow(
        String text, String languageCode, SessionsClient sessionsClient) {
      return dialogFlowMock;
    }

    @Override
    public UserService createUserService() {
      return userServiceMock;
    }

    @Override
    public DatastoreService createDatastore() {
      return customDatastore;
    }
  }

  private class TestableDFTextInputServlet extends TextInputServlet {
    @Override
    public UserService createUserService() {
      return userServiceMock;
    }
  }

  /**
   * Converts a json string into a map object
   *
   * @param json json string
   */
  public static Map<String, Value> stringToMap(String json) throws InvalidProtocolBufferException {
    JSONObject jsonObject = new JSONObject(json);
    Builder structBuilder = Struct.newBuilder();
    JsonFormat.parser().merge(jsonObject.toString(), structBuilder);
    Struct struct = structBuilder.build();
    return struct.getFieldsMap();
  }
}
