package com.google.sps.agents;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.protobuf.*;
import com.google.sps.data.BookQuery;
import com.google.sps.servlets.BookAgentServlet;
import java.text.ParseException;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class runs JUnit tests to test BookQuery object creation, given a map of parameters and user
 * inputs.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BookQueryTest {

  private static Logger log = LoggerFactory.getLogger(BookQueryTest.class);
  private String fullParameters;
  private String emptyParameters;
  private BookQuery fullQuery;
  private BookQuery emptyQuery;

  /**
   * Test setup which prepopulates the database with a bunch of default sessionIDs to test database
   * retrieval for different sessionIDs upon Book requests.
   */
  @Before
  public void setUp() throws ParseException, InvalidProtocolBufferException {
    try {
      fullParameters =
          "{\"number\" : 3,"
              + "\"order\" : \"newest\","
              + "\"title\" : \"the fault in our stars\","
              + "\"type\" : \"book\","
              + "\"authors\" : [{\"name\": \"michael mapes\"}, { \"name\": \"abby mapes\" }],"
              + "\"categories\" : \"romance\","
              + "\"language\" : \"French\"}";
      fullQuery =
          BookQuery.createBookQuery(
              "search",
              "show me newest books about love",
              BookAgentServlet.stringToMap(fullParameters));

      emptyParameters =
          "{\"number\" : 3,"
              + "\"order\" : \"\","
              + "\"title\" : \"\","
              + "\"type\" : \"\","
              + "\"authors\" : [],"
              + "\"categories\" : \"\","
              + "\"language\" : \"\"}";
      emptyQuery =
          BookQuery.createBookQuery(
              "search", "Show me an empty search", BookAgentServlet.stringToMap(emptyParameters));
    } catch (IllegalArgumentException e) {
      Assert.fail("Should not have thrown any exception in set up");
    }
  }

  /**
   * Checks that if the userInput is empty, then an exception is thrown and a BookQuery object is
   * not created.
   */
  @Test(expected = IllegalArgumentException.class)
  public void checkInvalidBookQuery()
      throws IllegalArgumentException, InvalidProtocolBufferException {
    BookQuery invalidQuery =
        BookQuery.createBookQuery("", "", BookAgentServlet.stringToMap(emptyParameters));
  }

  /**
   * Checks that the userInput property is correct for a BookQuery object with all properties
   * specified, and a BookQuery object with no properties specified.
   */
  @Test
  public void checkUserInput() throws Exception {
    assertEquals("show me newest books about love", fullQuery.getUserInput());
    assertEquals("Show me an empty search", emptyQuery.getUserInput());
  }

  /**
   * Checks that the queryString property is correct for a BookQuery object with all properties
   * specified, and a BookQuery object with no properties specified.
   */
  @Test
  public void checkQueryString() throws Exception {
    assertEquals(
        "newest+books+about+love+inauthor:\"michael+mapes\"+inauthor:\"abby+mapes\"+intitle:\"the+fault+in+our+stars\"",
        fullQuery.getQueryString());
    assertEquals("an+empty+search", emptyQuery.getQueryString());
  }

  /**
   * Checks that the title property is correct for a BookQuery object with all properties specified,
   * and a BookQuery object with no properties specified.
   */
  @Test
  public void checkTitle() throws Exception {
    assertEquals("intitle:\"the+fault+in+our+stars\"", fullQuery.getTitle());
    assertNull(emptyQuery.getTitle());
  }

  /**
   * Checks that the authors property is correct for a BookQuery object with all properties
   * specified, and a BookQuery object with no properties specified.
   */
  @Test
  public void checkAuthors() throws Exception {
    assertEquals("inauthor:\"michael+mapes\"+inauthor:\"abby+mapes\"", fullQuery.getAuthors());
    assertNull(emptyQuery.getAuthors());
  }

  /**
   * Checks that the type property is correct for a BookQuery object with all properties specified,
   * and a BookQuery object with no properties specified.
   */
  @Test
  public void checkType() throws Exception {
    assertEquals("book", fullQuery.getType());
    assertNull(emptyQuery.getType());
  }

  /**
   * Checks that the categories property is correct for a BookQuery object with all properties
   * specified, and a BookQuery object with no properties specified.
   */
  @Test
  public void checkCategories() throws Exception {
    assertEquals("romance", fullQuery.getCategories());
    assertNull(emptyQuery.getCategories());
  }

  /**
   * Checks that the order property is correct for a BookQuery object with all properties specified,
   * and a BookQuery object with no properties specified.
   */
  @Test
  public void checkOrder() throws Exception {
    assertEquals("newest", fullQuery.getOrder());
    assertNull(emptyQuery.getOrder());
  }

  /**
   * Checks that the language property is correct for a BookQuery object with all properties
   * specified, and a BookQuery object with no properties specified.
   */
  @Test
  public void checkLanguage() throws Exception {
    assertEquals("fr", fullQuery.getLanguage());
    assertNull(emptyQuery.getLanguage());
  }
}
