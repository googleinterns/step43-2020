package com.google.sps.agents;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.api.services.books.v1.model.Volume;
import com.google.api.services.books.v1.model.Volumes;
import com.google.protobuf.*;
import com.google.sps.data.Book;
import com.google.sps.data.BookQuery;
import com.google.sps.servlets.BookAgentServlet;
import com.google.sps.utils.BookUtils;
import java.text.ParseException;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This class runs JUnit tests to test that given a BookQuery object with parameters specified, the
 * appropriate Volumes are retrieved from Google Books API and Book objects are created properly.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BookCreationTest {

  private String parameters;
  private BookUtils utils;

  @Before
  public void setUp() throws ParseException, InvalidProtocolBufferException {
    utils = new BookUtils();
  }

  /**
   * Checks that given a BookQuery object with the author property specified, correct Volume objects
   * are returned from the Google Books API
   */
  @Test
  public void checkAuthorSpecifiedQuery() throws Exception {
    parameters =
        "{\"order\" : \"\","
            + "\"title\" : \"\","
            + "\"type\" : \"book\","
            + "\"authors\" : [{\"name\": \"john green\"}],"
            + "\"categories\" : \"\","
            + "\"language\" : \"\"}";
    BookQuery query =
        BookQuery.createBookQuery(
            "show me books by john green", BookAgentServlet.stringToMap(parameters));
    ArrayList<Book> books = utils.getRequestedBooks(query, 0);
    for (Book book : books) {
      assertTrue(book.getAuthors().contains("John Green"));
    }
  }

  /**
   * Checks that given a BookQuery object with the title property specified, correct Volume objects
   * are returned from the Google Books API
   */
  @Test
  public void checkTitleSpecifiedQuery() throws Exception {
    parameters =
        "{\"order\" : \"\","
            + "\"title\" : \"The Last\","
            + "\"type\" : \"book\","
            + "\"authors\" : [],"
            + "\"categories\" : \"\","
            + "\"language\" : \"\"}";
    BookQuery query =
        BookQuery.createBookQuery(
            "show me books titled the last", BookAgentServlet.stringToMap(parameters));
    ArrayList<Book> books = utils.getRequestedBooks(query, 0);
    for (Book book : books) {
      assertTrue(book.getTitle().toLowerCase().contains("the last"));
    }
  }

  /**
   * Checks that given a BookQuery object with the type property specified as "books", only Volume
   * objects classified as "books" are returned from the Google Books API
   */
  @Test
  public void checkTypeSpecifiedQuery1() throws Exception {
    parameters =
        "{\"order\" : \"\","
            + "\"title\" : \"\","
            + "\"type\" : \"books\","
            + "\"authors\" : [],"
            + "\"categories\" : \"\","
            + "\"language\" : \"\"}";
    BookQuery query =
        BookQuery.createBookQuery("show me books", BookAgentServlet.stringToMap(parameters));
    Volumes volumes = utils.getVolumes(query, 0);
    ArrayList<Volume> vols = new ArrayList<Volume>(volumes.getItems());
    for (Volume vol : vols) {
      assertTrue(vol.getVolumeInfo().getPrintType().equals("BOOK"));
    }
  }

  /**
   * Checks that given a BookQuery object with the type property specified as "magazines", only
   * Volume objects classified as "magazines" are returned from the Google Books API
   */
  @Test
  public void checkTypeSpecifiedQuery2() throws Exception {
    parameters =
        "{\"order\" : \"\","
            + "\"title\" : \"\","
            + "\"type\" : \"magazines\","
            + "\"authors\" : [],"
            + "\"categories\" : \"\","
            + "\"language\" : \"\"}";
    BookQuery query =
        BookQuery.createBookQuery(
            "show me magazines for children", BookAgentServlet.stringToMap(parameters));
    Volumes volumes = utils.getVolumes(query, 0);
    ArrayList<Volume> vols = new ArrayList<Volume>(volumes.getItems());
    for (Volume vol : vols) {
      assertTrue(vol.getVolumeInfo().getPrintType().equals("MAGAZINE"));
    }
  }

  /**
   * Checks that given a BookQuery object with the type property specified as "ebooks", only Volume
   * objects classified as "ebooks" are returned from the Google Books API
   */
  @Test
  public void checkTypeSpecifiedQuery3() throws Exception {
    parameters =
        "{\"order\" : \"\","
            + "\"title\" : \"\","
            + "\"type\" : \"ebooks\","
            + "\"authors\" : [],"
            + "\"categories\" : \"\","
            + "\"language\" : \"\"}";
    BookQuery query =
        BookQuery.createBookQuery("show me ebooks", BookAgentServlet.stringToMap(parameters));
    Volumes volumes = utils.getVolumes(query, 0);
    ArrayList<Volume> vols = new ArrayList<Volume>(volumes.getItems());
    for (Volume vol : vols) {
      assertTrue(vol.getSaleInfo().getIsEbook());
    }
  }

  /**
   * Checks that given a BookQuery object with the type property specified as "free-ebooks", only
   * Volume objects classified as "free-ebooks" are returned from the Google Books API
   */
  @Test
  public void checkTypeSpecifiedQuery4() throws Exception {
    parameters =
        "{\"order\" : \"\","
            + "\"title\" : \"\","
            + "\"type\" : \"free-ebooks\","
            + "\"authors\" : [],"
            + "\"categories\" : \"\","
            + "\"language\" : \"\"}";
    BookQuery query =
        BookQuery.createBookQuery(
            "show me free ebooks about love", BookAgentServlet.stringToMap(parameters));
    Volumes volumes = utils.getVolumes(query, 0);
    ArrayList<Volume> vols = new ArrayList<Volume>(volumes.getItems());
    for (Volume vol : vols) {
      assertTrue(vol.getSaleInfo().getIsEbook());
    }
  }

  /**
   * Checks that given a BookQuery object with the language property specified, only Volume objects
   * with the specified language are returned from the Google Books API
   */
  @Test
  public void checkLanguageSpecifiedQuery() throws Exception {
    parameters =
        "{\"order\" : \"\","
            + "\"title\" : \"\","
            + "\"type\" : \"\","
            + "\"authors\" : [],"
            + "\"categories\" : \"\","
            + "\"language\" : \"French\"}";
    BookQuery query =
        BookQuery.createBookQuery(
            "show me books about love", BookAgentServlet.stringToMap(parameters));
    Volumes volumes = utils.getVolumes(query, 0);
    ArrayList<Volume> vols = new ArrayList<Volume>(volumes.getItems());
    for (Volume vol : vols) {
      assertEquals("fr", vol.getVolumeInfo().getLanguage());
    }
  }
}
