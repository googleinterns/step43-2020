package com.google.sps.agents;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.sps.data.Output;
import com.google.sps.servlets.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.Silent.class)
public class NameTest {

  private static Logger log = LoggerFactory.getLogger(Name.class);

  @Test
  public void testNotLoggedIn() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"Tom\","
            + "\"type\": \"\"}";
    TestHelper tester = new TestHelper("Change my name to Tom.", jsonParams, "name.change");
    tester.setLoggedOut();

    Output output = tester.getOutput();

    assertEquals("Please login to modify your name.", output.getFulfillmentText());
    assertNull(output.getDisplay());
  }

  @Test
  public void testGeneralName() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"Tom\","
            + "\"type\": \"\"}";
    TestHelper tester = new TestHelper("Change my name to Tom.", jsonParams, "name.change");

    Output output = tester.getOutput();

    assertEquals("Changing your first name to be Tom.", output.getFulfillmentText());
    assertEquals("Tom", output.getDisplay());
  }

  @Test
  public void testFirstName() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"Tom\","
            + "\"type\": \"first name\"}";
    TestHelper tester = new TestHelper("Change my first name to Tom.", jsonParams, "name.change");

    Output output = tester.getOutput();

    assertEquals("Changing your first name to be Tom.", output.getFulfillmentText());
    assertEquals("Tom", output.getDisplay());
  }

  @Test
  public void testNickName() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"Tom\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"\","
            + "\"type\": \"nickname\"}";
    TestHelper tester = new TestHelper("Change my nickname to Tom.", jsonParams, "name.change");

    Output output = tester.getOutput();

    assertEquals("Changing your nickname to be Tom.", output.getFulfillmentText());
    assertEquals("Tom", output.getDisplay());
  }

  @Test
  public void testNickNameGiven() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"Tom\","
            + "\"type\": \"nickname\"}";
    TestHelper tester = new TestHelper("Change my nickname to Tom.", jsonParams, "name.change");

    Output output = tester.getOutput();

    assertEquals("Changing your nickname to be Tom.", output.getFulfillmentText());
    assertEquals("Tom", output.getDisplay());
  }

  @Test
  public void testOnlyLastName() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"Tom\","
            + "\"given-name\": \"\","
            + "\"type\": \"last name\"}";
    TestHelper tester = new TestHelper("Change my last name to Tom.", jsonParams, "name.change");

    Output output = tester.getOutput();

    assertEquals("Changing your last name to be Tom.", output.getFulfillmentText());
    assertEquals("test@example.com", output.getDisplay());
  }

  @Test
  public void testDidNotHearName() throws Exception {

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"\","
            + "\"type\": \"nickname\"}";
    TestHelper tester = new TestHelper("Change my nickname to Tom.", jsonParams, "name.change");

    Output output = tester.getOutput();

    assertEquals(
        "I'm sorry, I didn't catch the name. Can you repeat that?", output.getFulfillmentText());
    assertNull(output.getDisplay());
  }

  @Test
  public void testConsecutiveChanges() throws Exception {

    // Set name to be Tom -- should output display name as Tom since first name change

    String jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"Tom\","
            + "\"type\": \"\"}";

    TestHelper tester = new TestHelper("Change my name to Tom.", jsonParams, "name.change");

    Output output = tester.getOutput();

    assertEquals("Changing your first name to be Tom.", output.getFulfillmentText());
    assertEquals("Tom", output.getDisplay());

    // Set nickname to be NicknameTom -- should output display name as NicknameTom since nickname >
    // name

    jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"NicknameTom\","
            + "\"type\": \"nickname\"}";
    tester.setParameters("Change my nickname to Tom.", jsonParams, "name.change");

    output = tester.getOutput();

    assertEquals("Changing your nickname to be NicknameTom.", output.getFulfillmentText());
    assertEquals("NicknameTom", output.getDisplay());

    // Set name to be NameTom -- should output display name as NicknameTom since nickname exists and
    // nickname > name

    jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"NameTom\","
            + "\"type\": \"first name\"}";
    tester.setParameters("Change my first name to NameTom.", jsonParams, "name.change");

    output = tester.getOutput();

    assertEquals("Changing your first name to be NameTom.", output.getFulfillmentText());
    assertEquals("NicknameTom", output.getDisplay());

    // Set last name to be LastNameTom -- should output display name as NicknameTom since last name
    // never displayed

    jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"LastNameTom\","
            + "\"type\": \"last name\"}";
    tester.setParameters("Change my first name to NameTom.", jsonParams, "name.change");

    output = tester.getOutput();

    assertEquals("Changing your last name to be LastNameTom.", output.getFulfillmentText());
    assertEquals("NicknameTom", output.getDisplay());

    // Set nickname to be NewNicknameTom -- should output display name as NicknameTom since nickname
    // > name

    jsonParams =
        "{\"nick-name\": \"\","
            + "\"last-name\": \"\","
            + "\"given-name\": \"NewNicknameTom\","
            + "\"type\": \"nickname\"}";
    tester.setParameters("Change my nickname to NewNicknameTom.", jsonParams, "name.change");

    output = tester.getOutput();

    assertEquals("Changing your nickname to be NewNicknameTom.", output.getFulfillmentText());
    assertEquals("NewNicknameTom", output.getDisplay());
  }
}
