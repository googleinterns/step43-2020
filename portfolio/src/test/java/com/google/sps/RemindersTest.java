package com.google.sps.servlets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.sps.data.Output;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RemindersTest {

  @Test
  public void testSnooze10Sec() throws Exception {

    TestHelper tester =
        new TestHelper(
            // User input text
            "Set a timer for 10 sec",
            // Parameter JSON string (copy paste from dialogflow)
            "{\"date-time\": {\"endDateTime\": \"2020-06-29T16:53:50-07:00\",\"startDateTime\": \"2020-06-29T16:53:40-07:00\"}}",
            // Intent that you expect dialogflow to return based on your query
            "reminders.snooze");

    Output output = tester.getOutput();

    // Assertions
    assertEquals("Starting a timer for 10 seconds now.", output.getFulfillmentText());
    assertEquals("0:10", output.getDisplay());
  }

  @Test
  public void testSnooze1Min() throws Exception {

    TestHelper tester =
        new TestHelper(
            "Set a timer for 1 min",
            "{\"date-time\": {\"startDateTime\": \"2020-06-29T20:20:54-07:00\",\"endDateTime\": \"2020-06-29T20:21:54-07:00\"}}",
            "reminders.snooze");

    Output output = tester.getOutput();

    assertEquals("Starting a timer for 1 minute now.", output.getFulfillmentText());
    assertEquals("1:00", output.getDisplay());
  }

  @Test
  public void testSnooze10Hours() throws Exception {

    TestHelper tester =
        new TestHelper(
            "Set a timer for 10 hours",
            "{\"date-time\": {\"startDateTime\": \"2020-06-29T20:23:46-07:00\",\"endDateTime\": \"2020-06-30T06:23:46-07:00\"}}",
            "reminders.snooze");

    Output output = tester.getOutput();

    assertEquals("Starting a timer for 10 hours now.", output.getFulfillmentText());
    assertEquals("10:00:00", output.getDisplay());
  }

  @Test
  public void testSnoozeComplex() throws Exception {

    TestHelper tester =
        new TestHelper(
            "Set a timer for 15 hours, 37 min, and 4 sec",
            "{\"date-time\": {\"startDateTime\": \"2020-06-29T20:45:24-07:00\",\"endDateTime\": \"2020-06-30T12:22:28-07:00\"}}",
            "reminders.snooze");

    Output output = tester.getOutput();

    assertEquals(
        "Starting a timer for 15 hours 37 minutes and 4 seconds now.", output.getFulfillmentText());
    assertEquals("15:37:04", output.getDisplay());
  }

  @Test
  public void testSnoozeFailure1() throws Exception {

    TestHelper tester =
        new TestHelper(
            "Set a timer for 0 seconds.",
            "{\"date-time\": {\"startDateTime\": \"2020-06-29T20:45:24-07:00\",\"endDateTime\": \"2020-06-29T20:45:24-07:00\"}}",
            "reminders.snooze");

    Output output = tester.getOutput();

    assertNotEquals("Starting a timer for 0 seconds now.", output.getFulfillmentText());
    assertNotEquals("0:00", output.getDisplay());
    assertEquals(
        "Sorry, unable to set a timer for less than 1 second or more than 1 day. Please try adding a reminder instead.",
        output.getFulfillmentText());
  }

  @Test
  public void testSnoozeFailure2() throws Exception {

    TestHelper tester =
        new TestHelper(
            "Set a timer for 1 day and 1 second.",
            "{\"date-time\": {\"startDateTime\": \"2020-06-29T21:03:43-07:00\",\"endDateTime\": \"2020-06-30T21:03:44-07:00\"}}",
            "reminders.snooze");

    Output output = tester.getOutput();

    assertNotEquals("Starting a timer for 1 day and 1 second now.", output.getFulfillmentText());
    assertNotEquals("24:00:01", output.getDisplay());
    assertEquals(
        "Sorry, unable to set a timer for less than 1 second or more than 1 day. Please try adding a reminder instead.",
        output.getFulfillmentText());
  }
}
