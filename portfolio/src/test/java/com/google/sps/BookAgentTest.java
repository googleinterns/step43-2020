package com.google.sps.agents;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.protobuf.*;
import com.google.sps.data.Book;
import com.google.sps.data.BookQuery;
import com.google.sps.data.Output;
import com.google.sps.servlets.BookAgentServlet;
import com.google.sps.servlets.TestHelper;
import com.google.sps.utils.BooksAgentHelper;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class runs JUnit tests to test BookAgent outputs, given a mock Dialogflow response with
 * defined intents, fulfillment and user input phrases
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BookAgentTest {

  private static Logger log = LoggerFactory.getLogger(BookAgentTest.class);
  private TestHelper tester;
  private String parameters;
  private ArrayList<Book> books;

  /**
   * Test setup which prepopulates the database with a bunch of default sessionIDs to test database
   * retrieval for different sessionIDs upon Book requests.
   */
  @Before
  public void setUp() throws ParseException, InvalidProtocolBufferException {
    try {
      parameters =
          "{\"number\" : 3,"
              + "\"order\" : \"\","
              + "\"title\" : \"\","
              + "\"type\" : \"book\","
              + "\"authors\" : [],"
              + "\"categories\" : \"love\","
              + "\"language\" : \"\"}";
      BookQuery query =
          BookQuery.createBookQuery(
              "search", "Books about love", BookAgentServlet.stringToMap(parameters));
      tester = new TestHelper();

      // Pre-populate database for testSession1, testQuery1: invalid previous and valid more
      tester.setCustomDatabase( // startIndex
          0,
          // totalResults
          15,
          // resultsStored
          10,
          // displayNum
          5,
          // sessionID
          "testSession1",
          // queryID
          "testQuery1");
      tester.setCustomDatabase(query, "testSession1", "testQuery1");

      // Pre-populate database for testSession2, testQuery2: valid previous and invalid more
      tester.setCustomDatabase( // startIndex
          10,
          // totalResults
          10,
          // resultsStored
          10,
          // displayNum
          5,
          // sessionID
          "testSession2",
          // queryID
          "testQuery2");
      tester.setCustomDatabase(query, "testSession2", "testQuery2");

      // Pre-populate database for testSession2, testQuery3: valid previous and valid more
      tester.setCustomDatabase( // startIndex
          8,
          // totalResults
          200,
          // resultsStored
          10,
          // displayNum
          5,
          // sessionID
          "testSession1",
          // queryID
          "testQuery3");
      tester.setCustomDatabase(query, "testSession1", "testQuery3");

      // Pre-populate database with Books for testSession3, testSession2
      tester.setCustomDatabase( // startIndex
          0,
          // totalResults
          5,
          // resultsStored
          5,
          // displayNum
          5,
          // sessionID
          "testSession3",
          // queryID
          "testQuery1");
      tester.setCustomDatabase(query, "testSession3", "testQuery1");
      books = new ArrayList<Book>();
      books.add(new Book("Title 0", "Author 0a, Author 0b", "Description 0", true, "isbn0"));
      books.add(new Book("Title 1", "Author 1a, Author 1b", "Description 1", true, "isbn1"));
      books.add(new Book("Title 2", "Author 2a, Author 2b", "Description 2", true, "isbn2"));
      books.add(new Book("Title 3", "Author 3a, Author 3b", "Description 3", true, "isbn3"));
      books.add(new Book("Title 4", "Author 4a, Author 4b", "Description 4", true, "isbn4"));
      tester.setCustomDatabase(books, 0, "testSession3", "testQuery1");
    } catch (IOException | IllegalArgumentException e) {
      Assert.fail(
          "Should not have thrown any exception in set up valid BookQuery, Books and Indices set up.");
    }
  }

  @After
  public void deleteStoredInformation() {
    tester.deleteFromCustomDatabase("testSession1");
    tester.deleteFromCustomDatabase("testSession2");
    tester.deleteFromCustomDatabase("testSession3");
    tester.deleteFromCustomDatabase("fallbackTestingID");
  }

  /**
   * Checks that if the user query is invalid (i.e. one the Google Books API does not contain a
   * single match with), then the fulfillment is appropriate and no display is made.
   */
  @Test
  public void testEmptyQuery() throws Exception {
    tester.setParameters(
        "Books by abby mapes",
        "{\"order\" : \"\","
            + "\"title\" : \"\","
            + "\"type\" : \"book\","
            + "\"authors\" : [{\"name\": \"abby mapes\"}],"
            + "\"categories\" : \"\","
            + "\"language\" : \"\"}",
        "books.search");
    tester.setLoggedOut();
    Output output = tester.getOutput();
    assertEquals("I couldn't find any results. Can you try again?", output.getFulfillmentText());
    assertNull(output.getDisplay());
    assertNull(output.getRedirect());
  }

  /**
   * Checks that if the user books search query is valid, then the fulfillment is appropriate, and
   * both a redirect and display are made.
   */
  @Test
  public void testSearchQuery() throws Exception {
    tester.setParameters(
        "Books about puppies",
        "{\"order\" : \"\","
            + "\"title\" : \"\","
            + "\"type\" : \"book\","
            + "\"authors\" : [],"
            + "\"categories\" : \"puppies\","
            + "\"language\" : \"\"}",
        "books.search");
    tester.setLoggedOut();
    Output output = tester.getOutput();
    assertEquals("Here's what I found.", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
    assertNotNull(output.getRedirect());
  }

  /**
   * Checks requesting the previous page of a Book search query when there are no previous pages to
   * show. In this case, the fulfillment indicates that there are no previous pages, and both a
   * redirect and display are made.
   */
  @Test
  public void testInvalidPreviousRequest() throws Exception {
    Output output = tester.getOutput("books.previous", "testSession1", parameters, "testQuery1");
    assertEquals("This is the first page of results.", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
    assertEquals("testQuery1", output.getRedirect());
  }

  /**
   * Checks requesting the previous page of a Book search query when there are previous result pages
   * to show. In this case, the fulfillment indicates that it is showing the previous page, and both
   * a redirect and display are made.
   */
  @Test
  public void testValidPreviousRequest() throws Exception {
    Output output = tester.getOutput("books.previous", "testSession2", parameters, "testQuery2");
    assertEquals("Here's the previous page of results.", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
    assertEquals("testQuery2", output.getRedirect());
  }

  /**
   * Checks requesting the next page of a Book search query when there are no more pages to show. In
   * this case, the fulfillment indicates that there are no more pages, and both a redirect and
   * display are made.
   */
  @Test
  public void testInvalidMoreRequestCase() throws Exception {
    Output output = tester.getOutput("books.more", "testSession2", parameters, "testQuery2");
    assertEquals("This is the last page of results.", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
    assertEquals("testQuery2", output.getRedirect());
  }

  /**
   * Checks requesting the next page of a Book search query when there are more result pages to
   * show. In this case, the fulfillment indicates that it is showing the next page, and both a
   * redirect and display are made.
   *
   * <p>Checks the case where (startIndex + displayNum <= resultsStored)
   */
  @Test
  public void testValidMoreRequestCase1() throws Exception {
    Output output = tester.getOutput("books.more", "testSession1", parameters, "testQuery1");
    assertEquals("Here's the next page of results.", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
    assertEquals("testQuery1", output.getRedirect());
  }

  /**
   * Checks requesting the next page of a Book search query when there are more result pages to
   * show. In this case, the fulfillment indicates that it is showing the next page, and both a
   * redirect and display are made.
   *
   * <p>Checks the case where (startIndex + displayNum > resultsStored)
   */
  @Test
  public void testValidMoreRequestCase2() throws Exception {
    Output output = tester.getOutput("books.more", "testSession1", parameters, "testQuery3");
    assertEquals("Here's the next page of results.", output.getFulfillmentText());
    assertNotNull(output.getDisplay());
    assertEquals("testQuery3", output.getRedirect());
  }

  /** Checks requesting the description of a certain book, from parameter number. */
  @Test
  public void testBookDescription() throws Exception {
    Output output = tester.getOutput("books.description", "testSession3", parameters, "testQuery1");
    assertEquals("Here's a description of Title 3.", output.getFulfillmentText());
    assertEquals(BooksAgentHelper.bookToJson(books.get(3)), output.getDisplay());
    assertEquals("testQuery1", output.getRedirect());
  }

  /** Checks requesting the preview of a certain book, from parameter number. */
  @Test
  public void testBookPreview() throws Exception {
    Output output = tester.getOutput("books.preview", "testSession3", parameters, "testQuery1");
    assertEquals("Here's a preview of Title 3.", output.getFulfillmentText());
    assertEquals(BooksAgentHelper.bookToJson(books.get(3)), output.getDisplay());
    assertEquals("testQuery1", output.getRedirect());
  }

  /**
   * Checks requesting to return to the results, after viewing information about a particular book.
   */
  @Test
  public void testBookResults() throws Exception {
    Output output = tester.getOutput("books.results", "testSession3", parameters, "testQuery1");
    assertEquals("Here are the results.", output.getFulfillmentText());
    assertEquals(BooksAgentHelper.listToJson(books), output.getDisplay());
    assertEquals("testQuery1", output.getRedirect());
  }
}
