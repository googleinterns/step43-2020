package com.google.sps.agents;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.protobuf.Value;
import com.google.sps.servlets.TestHelper;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.Silent.class)
public class NameTest {

  @Mock UserService userServiceMock;

  @InjectMocks Name name;

  private static Logger log = LoggerFactory.getLogger(Name.class);

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before
  public void setUp() {
    userServiceMock = mock(UserService.class);
    when(userServiceMock.isUserLoggedIn()).thenReturn(true);
    when(userServiceMock.getCurrentUser())
        .thenReturn(new User("test@example.com", "authDomain", "1"));
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void testNotLoggedIn() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"Tom\","
            + "\"type\": \"\"}";
    Map<String, Value> params = TestHelper.stringToMap(jsonParams);

    when(userServiceMock.isUserLoggedIn()).thenReturn(false);

    TestableName nameAgent = new TestableName("name.user.change", params);

    assertEquals("Please login to modify your name.", nameAgent.getOutput());
    assertNull(nameAgent.getDisplay());
  }

  @Test
  public void testGeneralName() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"Tom\","
            + "\"type\": \"\"}";
    Map<String, Value> params = TestHelper.stringToMap(jsonParams);

    TestableName nameAgent = new TestableName("name.user.change", params);

    assertEquals("Changing your first name to be Tom.", nameAgent.getOutput());
    assertEquals("Tom", nameAgent.getDisplay());
  }

  @Test
  public void testFirstName() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"Tom\","
            + "\"type\": \"first name\"}";
    Map<String, Value> params = TestHelper.stringToMap(jsonParams);

    TestableName nameAgent = new TestableName("name.user.change", params);

    assertEquals("Changing your first name to be Tom.", nameAgent.getOutput());
    assertEquals("Tom", nameAgent.getDisplay());
  }

  @Test
  public void testNickName() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"Tom\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"\","
            + "\"type\": \"nickname\"}";
    Map<String, Value> params = TestHelper.stringToMap(jsonParams);

    TestableName nameAgent = new TestableName("name.user.change", params);

    assertEquals("Changing your nickname to be Tom.", nameAgent.getOutput());
    assertEquals("Tom", nameAgent.getDisplay());
  }

  @Test
  public void testNickNameGiven() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"Tom\","
            + "\"type\": \"nickname\"}";
    Map<String, Value> params = TestHelper.stringToMap(jsonParams);

    TestableName nameAgent = new TestableName("name.user.change", params);

    assertEquals("Changing your nickname to be Tom.", nameAgent.getOutput());
    assertEquals("Tom", nameAgent.getDisplay());
  }

  @Test
  public void testOnlyLastName() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"Tom\","
            + "\"given-name\": \"\","
            + "\"type\": \"last name\"}";
    Map<String, Value> params = TestHelper.stringToMap(jsonParams);

    TestableName nameAgent = new TestableName("name.user.change", params);

    assertEquals("Changing your last name to be Tom.", nameAgent.getOutput());
    assertEquals("test@example.com", nameAgent.getDisplay());
  }

  @Test
  public void testDidNotHearName() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"\","
            + "\"type\": \"nickname\"}";
    Map<String, Value> params = TestHelper.stringToMap(jsonParams);

    TestableName nameAgent = new TestableName("name.user.change", params);

    assertEquals("I'm sorry, I didn't catch the name. Can you repeat that?", nameAgent.getOutput());
    assertNull(nameAgent.getDisplay());
  }

  @Test
  public void testConsecutiveChanges() throws Exception {

    // Set name to be Tom -- should output display name as Tom since first name change

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"Tom\","
            + "\"type\": \"\"}";
    Map<String, Value> params = TestHelper.stringToMap(jsonParams);

    TestableName nameAgent = new TestableName("name.user.change", params);

    assertEquals("Changing your first name to be Tom.", nameAgent.getOutput());
    assertEquals("Tom", nameAgent.getDisplay());

    // Set nickname to be NicknameTom -- should output display name as NicknameTom since nickname >
    // name

    jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"NicknameTom\","
            + "\"type\": \"nickname\"}";
    params = TestHelper.stringToMap(jsonParams);

    nameAgent.setParameters(params);

    assertEquals("Changing your nickname to be NicknameTom.", nameAgent.getOutput());
    assertEquals("NicknameTom", nameAgent.getDisplay());

    // Set name to be NameTom -- should output display name as NicknameTom since nickname exists and
    // nickname > name

    jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"NameTom\","
            + "\"type\": \"first name\"}";
    params = TestHelper.stringToMap(jsonParams);

    nameAgent.setParameters(params);

    assertEquals("Changing your first name to be NameTom.", nameAgent.getOutput());
    assertEquals("NicknameTom", nameAgent.getDisplay());

    // Set last name to be LastNameTom -- should output display name as NicknameTom since last name
    // never displayed

    jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"LastNameTom\","
            + "\"type\": \"last name\"}";
    params = TestHelper.stringToMap(jsonParams);

    nameAgent.setParameters(params);

    assertEquals("Changing your last name to be LastNameTom.", nameAgent.getOutput());
    assertEquals("NicknameTom", nameAgent.getDisplay());

    // Set nickname to be NewNicknameTom -- should output display name as NicknameTom since nickname
    // > name

    jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"NewNicknameTom\","
            + "\"type\": \"nickname\"}";
    params = TestHelper.stringToMap(jsonParams);

    nameAgent.setParameters(params);

    assertEquals("Changing your nickname to be NewNicknameTom.", nameAgent.getOutput());
    assertEquals("NewNicknameTom", nameAgent.getDisplay());
  }

  private class TestableName extends Name {

    TestableName(String intentName, Map<String, Value> parameters) {
      super(intentName, parameters);
    }

    @Override
    public UserService createUserService() {
      return userServiceMock;
    }
  }
}
