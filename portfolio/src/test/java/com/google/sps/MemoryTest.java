package com.google.sps.agents;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.protobuf.*;
import com.google.sps.data.Output;
import com.google.sps.servlets.TestHelper;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MemoryTest {

  private static Logger log = LoggerFactory.getLogger(MemoryTest.class);
  private TestHelper tester;
  List<String> commentList;

  /**
   * Test setup which prepopulates the database with a bunch of default values to test database
   * retrieval for different keywords and time durations.
   */
  @Before
  public void setUp() {
    commentList =
        Arrays.asList(
            "hello",
            "Hello!",
            "tell me a joke",
            "A bus is a vehicle that runs twice as fast when you are after it as when you are in it.",
            "search conversation history for the word apple",
            "Sorry, unable to find any results including the keyword \"apple.\"",
            "Here is a sentence with two words in the same SENTENCE",
            "hello",
            "test1",
            "test2",
            "test3",
            "test4",
            "test5",
            "test6",
            "test7");
    tester = new TestHelper();
    tester.setCustomDatabase(commentList);
  }

  /** Checks that no display output or database query is made if the user is not logged in. */
  @Test
  public void testNotLoggedIn() throws Exception {
    tester.setParameters(
        "Search conversation history for the word hello.",
        "{\"keyword\":\"hello\"}",
        "memory.keyword");
    tester.setLoggedOut();

    Output output = tester.getOutput();

    assertEquals("Please login to access conversation history.", output.getFulfillmentText());
    assertNull(output.getDisplay());

    tester.setLoggedIn();
  }

  /**
   * Tests a search for the keyword hello: - should identify the first hello at index 0 and
   * surrounding conversation of comments at indices 0 - 6 - verifies that surrounding conversation
   * feature works if there are no comments before identified comment - should identify the second
   * hello at index 1 and surrounding conversation at indices 0 - 7 - verifies that program
   * identifies keywords even if capitalization is different - should identiy the third hello at
   * index 7 and surrounding conversation at indices 1 - 14 - verifies that surrounding conversation
   * feature works and grabs 6 comments before and after identified comment
   */
  @Test
  public void testGeneral() throws Exception {

    tester.setParameters(
        "Search conversation history for the word hello.",
        "{\"keyword\":\"hello\"}",
        "memory.keyword");

    Output output = tester.getOutput();
    assertEquals(
        "Here are all the results including the keyword \"hello.\"", output.getFulfillmentText());

    Map<String, Value> result = TestHelper.stringToMap(output.getDisplay());
    List<Value> identifiedCommentList =
        result.get("conversationList").getListValue().getValuesList();
    assertEquals(3, identifiedCommentList.size());

    List<List<String>> allComments = unpackAllComments(identifiedCommentList, "hello");

    // "hello"
    List<String> neighbors = allComments.get(0);
    assertEquals(7, neighbors.size());
    for (int i = 0; i < neighbors.size(); i++) {
      assertEquals(commentList.get(i), neighbors.get(i));
    }

    // "Hello!"
    neighbors = allComments.get(1);
    assertEquals(8, neighbors.size());
    for (int i = 0; i < neighbors.size(); i++) {
      assertEquals(commentList.get(i), neighbors.get(i));
    }

    // "hello" (second)
    neighbors = allComments.get(2);
    assertEquals(13, neighbors.size());
    for (int i = 0; i < neighbors.size(); i++) {
      assertEquals(commentList.get(i + 1), neighbors.get(i));
    }
  }

  /**
   * Tests a search for the keyword apple: - should identify the first apple at index 4 - should
   * identify the second apple at index 5 - verifies that program identifies keywords inside
   * quotations (mainly that \" symbol doesn't break anything)
   */
  @Test
  public void testQuoteAroundWord() throws Exception {

    tester.setParameters(
        "Search conversation history for the word apple.",
        "{\"keyword\":\"apple\"}",
        "memory.keyword");

    Output output = tester.getOutput();
    assertEquals(
        "Here are all the results including the keyword \"apple.\"", output.getFulfillmentText());

    Map<String, Value> result = TestHelper.stringToMap(output.getDisplay());
    List<Value> identifiedCommentList =
        result.get("conversationList").getListValue().getValuesList();
    assertEquals(2, identifiedCommentList.size());

    List<List<String>> allComments = unpackAllComments(identifiedCommentList, "apple");

    // "apple" (first)
    List<String> neighbors = allComments.get(0);
    assertEquals(11, neighbors.size());
    for (int i = 0; i < neighbors.size(); i++) {
      assertEquals(commentList.get(i), neighbors.get(i));
    }

    // "apple" (second)
    neighbors = allComments.get(1);
    assertEquals(12, neighbors.size());
    for (int i = 0; i < neighbors.size(); i++) {
      assertEquals(commentList.get(i), neighbors.get(i));
    }
  }

  /**
   * Tests a search for the keyword sentence: - should identify the comment at line 6 - verifies
   * that program identifies only one comment even though keyword appears twice in that comment
   */
  @Test
  public void testWordTwiceInOneSentence() throws Exception {

    tester.setParameters(
        "Search conversation history for the word sentence.",
        "{\"keyword\":\"sentence\"}",
        "memory.keyword");

    Output output = tester.getOutput();
    assertEquals(
        "Here are all the results including the keyword \"sentence.\"",
        output.getFulfillmentText());

    Map<String, Value> result = TestHelper.stringToMap(output.getDisplay());
    List<Value> identifiedCommentList =
        result.get("conversationList").getListValue().getValuesList();
    assertEquals(1, identifiedCommentList.size());

    List<List<String>> allComments = unpackAllComments(identifiedCommentList, "sentence");

    // "sentence"
    List<String> neighbors = allComments.get(0);
    assertEquals(13, neighbors.size());
    for (int i = 0; i < neighbors.size(); i++) {
      assertEquals(commentList.get(i), neighbors.get(i));
    }
  }

  /**
   * Tests a search for the keyword test: - should identify all comments after line 8 - verifies
   * that surrounding conversation feature works for identified comments that have no surrounding
   * comments after it
   */
  @Test
  public void testNoSurroundingCommentAfter() throws Exception {

    tester.setParameters(
        "Search conversation history for the word test.",
        "{\"keyword\":\"test\"}",
        "memory.keyword");

    Output output = tester.getOutput();
    assertEquals(
        "Here are all the results including the keyword \"test.\"", output.getFulfillmentText());

    Map<String, Value> result = TestHelper.stringToMap(output.getDisplay());
    List<Value> identifiedCommentList =
        result.get("conversationList").getListValue().getValuesList();
    assertEquals(7, identifiedCommentList.size());

    List<List<String>> allComments = unpackAllComments(identifiedCommentList, "test");

    for (int i = 0; i < allComments.size(); i++) {
      List<String> neighbors = allComments.get(i);
      assertEquals(13 - i, neighbors.size());
      for (int j = 0; j < neighbors.size(); j++) {
        assertEquals(commentList.get(j + 2 + i), neighbors.get(j));
      }
    }
  }

  /**
   * Tests a search for the keyword blueberry: - verifies that a keyword that doesn't exist in
   * conversation history return a search not found
   */
  @Test
  public void testNonexistent() throws Exception {

    tester.setParameters(
        "Search conversation history for the word blueberry.",
        "{\"keyword\":\"blueberry\"}",
        "memory.keyword");

    Output output = tester.getOutput();
    assertEquals(
        "Sorry, there were no results matching the keyword \"blueberry.\"",
        output.getFulfillmentText());
    assertNull(output.getDisplay());
  }

  private List<List<String>> unpackAllComments(List<Value> identifiedCommentList, String word) {
    List<List<String>> allComments = new ArrayList<>();

    for (Value listObject : identifiedCommentList) {
      Map<String, Value> commentGroup = listObject.getStructValue().getFieldsMap();
      String identifiedComment = getComment(commentGroup.get("key"));
      assertTrue(identifiedComment.toLowerCase().contains(word.toLowerCase()));

      List<Value> surroundingComments = commentGroup.get("value").getListValue().getValuesList();
      List<String> comments =
          surroundingComments.stream()
              .map((surroundingCommentEntity) -> getComment(surroundingCommentEntity))
              .collect(Collectors.toList());
      allComments.add(comments);
    }
    return allComments;
  }

  private String getComment(Value commentEntity) {
    return commentEntity
        .getStructValue()
        .getFieldsMap()
        .get("propertyMap")
        .getStructValue()
        .getFieldsMap()
        .get("comment")
        .getStringValue();
  }
}
