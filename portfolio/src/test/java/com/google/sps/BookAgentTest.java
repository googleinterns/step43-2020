package com.google.sps.agents;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.sps.data.Book;
import com.google.sps.servlets.BookTestHelper;
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
  private BookTestHelper bookTester;
  private String parameters;
  private ArrayList<Book> books, expectedFirstPage, expectedSecondPage;

  /**
   * Test setup which prepopulates the database with a bunch of default sessionIDs to test database
   * retrieval for different sessionIDs upon Book requests.
   */
  @Before
  public void setUp() throws ParseException, InvalidProtocolBufferException {
    try {
      this.bookTester = new BookTestHelper();
      this.parameters = "{\"number\" : 3," + "\"type\" : \"book\"," + "\"categories\" : \"love\"}";

      this.bookTester = new BookTestHelper();
      this.books = getBookList();
      this.expectedFirstPage = new ArrayList<Book>();
      for (int i = 0; i < 5; ++i) {
        expectedFirstPage.add(books.get(i));
      }
      this.expectedSecondPage = new ArrayList<Book>();
      for (int i = 5; i < books.size(); ++i) {
        expectedSecondPage.add(books.get(i));
      }
      // Initializes query-1 in database for testUser1 (books list is result)
      bookTester.setSearchBooks(books, 8);
      bookTester.setParameters("Books about frogs", parameters, "search", "testUser1");
    } catch (IOException | IllegalArgumentException e) {
      Assert.fail("Should not have thrown any exception in set up.");
    }
  }

  @After
  public void deleteStoredInformation() {
    bookTester.deleteFromCustomDatabase("testUser1");
  }

  /**
   * Checks that if the user's search query is invalid (i.e. one the Google Books API does not
   * contain a single match with), then the fulfillment is appropriate and no display is made.
   */
  @Test
  public void testFailedSearchIntent() throws Exception {
    bookTester.setSearchBooks(new ArrayList<Book>(), 0);
    bookTester.setParameters("Books by abby mapes", parameters, "search", "testUser1");
    assertEquals("I couldn't find any results. Can you try again?", bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * Checks that if the user books search query is valid, then the output, display, and redirect is
   * correct.
   */
  @Test
  public void testSuccessfulSearchIntent() throws Exception {
    bookTester.setParameters("Books about frogs", parameters, "search", "testUser1");
    ArrayList<Book> expectedList = new ArrayList<Book>();
    for (int i = 0; i < 5; ++i) {
      expectedList.add(books.get(i));
    }
    assertEquals("Here's what I found.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedFirstPage), bookTester.getDisplay());
    assertEquals("query-2", bookTester.getRedirect());
  }

  /**
   * Checks requesting the previous page of a Book search query when there are no previous pages to
   * show. In this case, the fulfillment indicates that there are no previous pages, and both a
   * redirect and display are made.
   */
  @Test
  public void testInvalidPreviousRequest() throws Exception {
    bookTester.setParameters(
        "previous page", parameters, "previous", "testUser1", "query-1", "test@example.com");
    assertEquals("This is the first page of results.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedFirstPage), bookTester.getDisplay());
    assertEquals("query-1", bookTester.getRedirect());
  }

  /**
   * Checks requesting the previous page of a Book search query when there are previous result pages
   * to show. In this case, the fulfillment indicates that it is showing the previous page, and both
   * a redirect and display are made.
   */
  @Test
  public void testValidPreviousRequest() throws Exception {
    bookTester.setParameters(
        "next page", parameters, "more", "testUser1", "query-1", "test@example.com");
    bookTester.setParameters(
        "previous page", parameters, "previous", "testUser1", "query-1", "test@example.com");
    assertEquals("Here's the previous page of results.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedFirstPage), bookTester.getDisplay());
    assertEquals("query-1", bookTester.getRedirect());
  }

  /**
   * Checks requesting the next page of a Book search query when there are no more pages to show. In
   * this case, the fulfillment indicates that there are no more pages, and both a redirect and
   * display are made.
   */
  @Test
  public void testInvalidMoreRequestCase() throws Exception {
    bookTester.setParameters(
        "next page", parameters, "more", "testUser1", "query-1", "test@example.com");
    bookTester.setParameters(
        "next page", parameters, "more", "testUser1", "query-1", "test@example.com");
    assertEquals("This is the last page of results.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedSecondPage), bookTester.getDisplay());
    assertEquals("query-1", bookTester.getRedirect());
  }

  /**
   * Checks requesting the next page of a Book search query when there are more result pages to
   * show. In this case, the fulfillment indicates that it is showing the next page, and both a
   * redirect and display are made.
   *
   * <p>Checks the case where (startIndex + displayNum >= totalResults)
   */
  @Test
  public void testValidMoreRequestCase1() throws Exception {
    bookTester.setParameters(
        "next page", parameters, "more", "testUser1", "query-1", "test@example.com");
    assertEquals("Here's the next page of results.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedSecondPage), bookTester.getDisplay());
    assertEquals("query-1", bookTester.getRedirect());
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
    bookTester.setSearchBooks(books, 16);
    bookTester.setParameters("Books about history", parameters, "search", "testUser1");
    // Makes call to API to retrieve more results
    bookTester.setParameters(
        "next page", parameters, "more", "testUser1", "query-2", "test@example.com");
    ArrayList<Book> expectedResults = getExpectedBookList();
    assertEquals("Here's the next page of results.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedResults), bookTester.getDisplay());
    assertEquals("query-2", bookTester.getRedirect());
  }

  /**
   * Checks requesting the next page of a Book search query when there are more result pages to
   * show. In this case, the fulfillment indicates that it is showing the next page, and both a
   * redirect and display are made.
   *
   * <p>Checks the case where (startIndex + displayNum = resultsStored)
   */
  @Test
  public void testValidMoreRequestCase3() throws Exception {
    books.add(new Book("Title 8", "Author 8a, Author 8b", "Description 8", true, "isbn6", 8));
    books.add(new Book("Title 9", "Author 9a, Author 9b", "Description 9", true, "isbn9", 9));
    bookTester.setSearchBooks(books, 10);
    bookTester.setParameters("Books about history", parameters, "search", "testUser1");
    bookTester.setParameters(
        "next page", parameters, "more", "testUser1", "query-2", "test@example.com");
    ArrayList<Book> expectedResults = new ArrayList<Book>();
    for (int i = 5; i < books.size(); ++i) {
      expectedResults.add(books.get(i));
    }
    assertEquals("Here's the next page of results.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedResults), bookTester.getDisplay());
    assertEquals("query-2", bookTester.getRedirect());
  }

  /** Checks requesting the description of a certain book, from parameter number. */
  @Test
  public void testBookDescription() throws Exception {
    bookTester.setParameters(
        "description of title 3",
        parameters,
        "description",
        "testUser1",
        "query-1",
        "test@example.com");
    assertEquals("Here's a description of Title 3.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.bookToJson(books.get(3)), bookTester.getDisplay());
    assertEquals("query-1", bookTester.getRedirect());
  }

  /** Checks requesting the preview of a certain book, from parameter number. */
  @Test
  public void testBookPreview() throws Exception {
    bookTester.setParameters(
        "preview of title 3", parameters, "preview", "testUser1", "query-1", "test@example.com");
    assertEquals("Here's a preview of Title 3.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.bookToJson(books.get(3)), bookTester.getDisplay());
    assertEquals("query-1", bookTester.getRedirect());
  }

  /**
   * Checks requesting to return to the results, after viewing information about a particular book.
   */
  @Test
  public void testBookResults() throws Exception {
    bookTester.setParameters(
        "back to results", parameters, "results", "testUser1", "query-1", "test@example.com");
    assertEquals("Here are the results.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedFirstPage), bookTester.getDisplay());
    assertEquals("query-1", bookTester.getRedirect());
  }

  private ArrayList<Book> getExpectedBookList() {
    ArrayList<Book> expectedResults = expectedSecondPage;
    expectedResults.add(books.get(0));
    expectedResults.add(books.get(1));
    expectedResults.get(0).setOrder(5);
    expectedResults.get(1).setOrder(6);
    expectedResults.get(2).setOrder(7);
    expectedResults.get(3).setOrder(8);
    expectedResults.get(4).setOrder(9);
    return expectedResults;
  }

  public static ArrayList<Book> getBookList() {
    ArrayList<Book> books = new ArrayList<Book>();
    books.add(new Book("Title 0", "Author 0a, Author 0b", "Description 0", true, "isbn0", 0));
    books.add(new Book("Title 1", "Author 1a, Author 1b", "Description 1", true, "isbn1", 1));
    books.add(new Book("Title 2", "Author 2a, Author 2b", "Description 2", true, "isbn2", 2));
    books.add(new Book("Title 3", "Author 3a, Author 3b", "Description 3", true, "isbn3", 3));
    books.add(new Book("Title 4", "Author 4a, Author 4b", "Description 4", true, "isbn4", 4));
    books.add(new Book("Title 5", "Author 5a, Author 5b", "Description 5", true, "isbn5", 5));
    books.add(new Book("Title 6", "Author 6a, Author 6b", "Description 6", true, "isbn6", 6));
    books.add(new Book("Title 7", "Author 7a, Author 7b", "Description 7", true, "isbn7", 7));
    return books;
  }
}
