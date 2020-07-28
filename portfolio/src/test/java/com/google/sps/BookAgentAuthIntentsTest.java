package com.google.sps.agents;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.sps.data.Book;
import com.google.sps.data.Friend;
import com.google.sps.servlets.BookTestHelper;
import com.google.sps.utils.BooksAgentHelper;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class runs JUnit tests to test BookAgent outputs for intents that require user to be logged
 * in given mock BookUtils, PeopleUtils, and OAuthUtils instances to mock Google Books API, Google
 * People API, and OAuth2.0
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BookAgentAuthIntentsTest {

  private static Logger log = LoggerFactory.getLogger(BookAgentAuthIntentsTest.class);
  private BookTestHelper bookTester;
  private ArrayList<Book> bookshelfBooks;
  private ArrayList<String> bookshelves;
  private ArrayList<Friend> friends;
  private ArrayList<Book> friendsLikes;
  private Book firstBook, secondBook, thirdBook, fourthBook, fifthBook, sixthBook;

  /**
   * Test setup which prepopulates the database and mocks with appropriate information about the
   * authenticated user
   */
  @Before
  public void setUp() throws ParseException, InvalidProtocolBufferException {
    try {
      // Set authenticated user
      this.bookTester = new BookTestHelper();
      bookTester.setLoggedIn("authUser1@gmail.com", "authUser1");
      bookTester.setAuthenticatedUser("authUser1");

      // Set user bookshelves and books in bookshelf for BookUtils mock
      this.bookshelves =
          new ArrayList<String>(
              Arrays.asList(
                  "My Google eBooks",
                  "Purchased",
                  "Reviewed",
                  "Recently viewed",
                  "Favorites",
                  "Reading now",
                  "To read",
                  "Have read",
                  "Books for you"));
      bookTester.setBookshelfNames("authUser1", bookshelves);
      this.bookshelfBooks = BookAgentTest.getBookList();
      bookTester.setBookShelfBooks("authUser1", bookshelfBooks, 8);

      // Set friends for PeopleUtils mock
      this.friends = getFriendList();
      bookTester.setFriends("authUser1", friends);

      // Populize database with friend's liked books
      setFriendsLikedBooks();

      // Set the proper liked by properties for each book in the expected list of friends likes
      // List will is sorted by like-count, ties broken alphabetically
      firstBook.setLikedBy(new ArrayList<String>(Arrays.asList("John Doe", "Jim Jones")));
      secondBook.setLikedBy(new ArrayList<String>(Arrays.asList("Mary Ann")));
      thirdBook.setLikedBy(new ArrayList<String>(Arrays.asList("Jane Smith")));
      fourthBook.setLikedBy(new ArrayList<String>(Arrays.asList("James Ray")));
      fifthBook.setLikedBy(new ArrayList<String>(Arrays.asList("John Doe")));
      sixthBook.setLikedBy(new ArrayList<String>(Arrays.asList("Jim Jones")));
      friendsLikes =
          new ArrayList<Book>(
              Arrays.asList(firstBook, secondBook, thirdBook, fourthBook, fifthBook, sixthBook));

      // Execute a library query for query-1-shelf in order to test followup intents for
      // query-1-shelf
      bookTester.setParameters(
          "Show me my favorites bookshelves",
          "{\"bookshelf\" : \"favorites\"}",
          "library",
          "authUser1",
          "query-1-shelf",
          "authUser1@gmail.com");

      // Set bookshelf editing accesses for BookUtils mock to true
      bookTester.setBookshelfEditingAccess("Favorites", "authUser1", true);

    } catch (IllegalArgumentException | IOException e) {
      Assert.fail("Should not have thrown any exception in set up.");
    }
  }

  @After
  public void deleteStoredInformation() {
    clearDatabase();
  }

  /**
   * This function tests a non-logged in user making a request for intents that require the user to
   * give proper authentication
   */
  @Test
  public void testUserNotLoggedIn() throws Exception {
    bookTester.setLoggedOut();
    bookTester.setParameters(
        "Show me my bookshelves", "{}", "library", "unloggedInUser", "testEmail@gmail.com");
    assertEquals("Please login first.", bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * This function tests a logged in, but non-authenticated user making a request for intents that
   * require the user to give proper authentication
   */
  @Test
  public void testUserNotAuthenticated() throws Exception {
    bookTester.setLoggedIn("testEmail@gmail.com", "unAuthenticatedUser");
    bookTester.setParameters(
        "Show me my bookshelves", "{}", "library", "unAuthenticatedUser", "testEmail@gmail.com");
    assertEquals(
        "Please allow me to access your Google Books and Contact information first.",
        bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * This function tests a when a properly authenticated user makes a request for library intent,
   * but does not specify which bookshelf in their library they would like to see in the parameters.
   */
  @Test
  public void testLibraryIntentWithNoBookshelf() throws Exception {
    bookTester.setParameters(
        "Show me my bookshelves", "{}", "library", "authUser1", "authUser1@gmail.com");
    String expectedOutput =
        "[\"My Google eBooks\",\"Purchased\",\"Reviewed\",\"Recently viewed\",\"Favorites\",\"Reading now\",\"To read\",\"Have read\",\"Books for you\"]";
    assertEquals("Which bookshelf would you like to see?", bookTester.getFulfillment());
    assertEquals(expectedOutput, bookTester.getDisplay());
    assertEquals("bookshelf-names", bookTester.getRedirect());
  }

  /**
   * This function tests the output, display, and redirect for a valid library request, requesting
   * books from a bookshelf in the authenticated user's libary.
   */
  @Test
  public void testValidLibraryIntent() throws Exception {
    bookTester.setParameters(
        "Show me my favorites bookshelves",
        "{\"bookshelf\" : \"favorites\"}",
        "library",
        "authUser1",
        "authUser1@gmail.com");
    ArrayList<Book> expectedList = new ArrayList<Book>();
    for (int i = 0; i < 5; ++i) {
      expectedList.add(bookshelfBooks.get(i));
    }
    String expectedOutput = BooksAgentHelper.listToJson(expectedList);
    assertEquals("Here are the books in your Favorites bookshelf.", bookTester.getFulfillment());
    assertEquals(expectedOutput, bookTester.getDisplay());
    assertEquals("query-2-shelf", bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a library intent when a user
   * requests books from one of their bookshelves that contains no books.
   */
  @Test
  public void testEmptyBookshelfLibraryIntent() throws Exception {
    bookTester.setBookShelfBooks("authUser1", new ArrayList<Book>(), 0);
    bookTester.setParameters(
        "Show me my favorites bookshelves",
        "{\"bookshelf\" : \"favorites\"}",
        "library",
        "authUser1",
        "authUser1@gmail.com");
    assertEquals("There are no books in your Favorites bookshelf.", bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for an add intent, when a user
   * requests to add a book to one of their bookshelves, but doesn't specify which bookshelf.
   */
  @Test
  public void testAddIntentWithNoBookshelf() throws Exception {
    bookTester.setParameters(
        "Add Title 2 to my bookshelf",
        "{\"number\" : 2}",
        "add",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    String expectedOutput = "[\"Favorites\",\"Reading now\",\"To read\",\"Have read\"]";
    assertEquals("Which bookshelf would you like to add Title 2 to?", bookTester.getFulfillment());
    assertEquals(expectedOutput, bookTester.getDisplay());
    assertEquals("query-1-shelf", bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a successful add intent when a
   * user requests to add a book to one of their bookshelves and the Google Books API does so.
   */
  @Test
  public void testSuccessfulAddIntent() throws Exception {
    bookTester.setParameters(
        "Add Title 2 to my bookshelf",
        "{\"number\" : 2, \"bookshelf\" : \"favorites\"}",
        "add",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    String expectedOutput = BooksAgentHelper.bookToJson(bookshelfBooks.get(2));
    assertEquals("I've added Title 2 to your Favorites bookshelf.", bookTester.getFulfillment());
    assertEquals(expectedOutput, bookTester.getDisplay());
    assertEquals("query-1-shelf", bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a failed add intent when a user
   * requests to add a book to one of their bookshelves and the Google Books API fails to do so.
   */
  @Test
  public void testFailedAddIntent() throws Exception {
    bookTester.setBookshelfEditingAccess("Favorites", "authUser1", false);
    bookTester.setParameters(
        "Add Title 2 to my favorites bookshelf",
        "{\"number\" : 2, \"bookshelf\" : \"favorites\"}",
        "add",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    String expectedOutput = BooksAgentHelper.bookToJson(bookshelfBooks.get(2));
    assertEquals(
        "I'm sorry. I couldn't add Title 2 to your Favorites bookshelf.",
        bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a valid delete intent when a user
   * requests to delete a book to one of their bookshelves and the Google Books API does so.
   */
  @Test
  public void testSuccessfulDeleteIntent() throws Exception {
    bookTester.setParameters(
        "Delete Title 4 from my bookshelf",
        "{\"number\" : 4, \"bookshelf\" : \"favorites\"}",
        "delete",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    String expectedOutput = BooksAgentHelper.bookToJson(bookshelfBooks.get(4));
    assertEquals(
        "I've deleted Title 4 from your Favorites bookshelf.", bookTester.getFulfillment());
    assertEquals(expectedOutput, bookTester.getDisplay());
    assertEquals("query-1-shelf", bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a failed delete intent when a
   * user requests to delete a book to one of their bookshelves and the Google Books API fails to do
   * so.
   */
  @Test
  public void testFailedDeleteIntent() throws Exception {
    bookTester.setBookshelfEditingAccess("Favorites", "authUser1", false);
    bookTester.setParameters(
        "Delete Title 4 from my favorites bookshelf",
        "{\"number\" : 4, \"bookshelf\" : \"favorites\"}",
        "delete",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    assertEquals(
        "I'm sorry. I couldn't delete Title 4 from your Favorites bookshelf.",
        bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a successful friends intent when
   * a user requests to see the books that their friends have liked.
   */
  @Test
  public void testSuccessfulFriendsIntent() throws Exception {
    bookTester.setParameters(
        "What books do my friends like?", "{}", "friends", "authUser1", "authUser1@gmail.com");
    assertEquals("Here are the books your friends like.", bookTester.getFulfillment());
    ArrayList<Book> expectedFriendsLikesDisplay = new ArrayList<Book>();
    for (int i = 0; i < 5; ++i) {
      expectedFriendsLikesDisplay.add(friendsLikes.get(i));
    }
    assertEquals(BooksAgentHelper.listToJson(expectedFriendsLikesDisplay), bookTester.getDisplay());
    assertEquals("query-2", bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a friends intent when a user
   * requests to see the books that their friends have liked, but their friends haven't liked any
   * books.
   */
  @Test
  public void testEmptyFriendsIntent() throws Exception {
    // Unlike liked books for authUser1 friends
    unlikeAllFriendsBooks();
    bookTester.setParameters(
        "What books do my friends like?", "{}", "friends", "authUser1", "authUser1@gmail.com");
    assertEquals("I'm sorry. Your friends haven't liked any books.", bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a successful mylikes intent when
   * a user requests to see their own liked books.
   */
  @Test
  public void testSuccessfulMyLikesIntent() throws Exception {
    bookTester.setLikedBook(firstBook, "authUser1", "authUser1@gmail.com");
    bookTester.setLikedBook(secondBook, "authUser1", "authUser1@gmail.com");
    bookTester.setLikedBook(thirdBook, "authUser1", "authUser1@gmail.com");

    // Set isLiked property for books in expectedLikes
    firstBook.setIsLiked(true);
    secondBook.setIsLiked(true);
    thirdBook.setIsLiked(true);
    ArrayList<Book> expectedLikes =
        new ArrayList<Book>(Arrays.asList(firstBook, secondBook, thirdBook));

    bookTester.setParameters(
        "Show me my liked books", "{}", "mylikes", "authUser1", "authUser1@gmail.com");
    assertEquals("Here are your liked books.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedLikes), bookTester.getDisplay());
    assertEquals("query-2", bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a mylikes intent when a user
   * requests to see their own liked books, but they haven't liked any books.
   */
  @Test
  public void testEmptyMyLikesIntent() throws Exception {
    bookTester.setParameters(
        "Show me my liked books", "{}", "mylikes", "authUser1", "authUser1@gmail.com");
    assertEquals("You haven't liked any books yet!", bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a successful friendlikes intent
   * when a user requests to see the liked books of one of their friends, but doesn't specify a
   * friend parameter.
   */
  @Test
  public void testFriendLikesIntentInvalidParameter() throws Exception {
    bookTester.setParameters(
        "Show me my friend's likes",
        "{\"friend\" : {\"name\": \"\"}}",
        "friendlikes",
        "authUser1",
        "authUser1@gmail.com");
    assertEquals("Which friend?", bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a successful friendlikes intent
   * when a user requests to see the liked books of one of their friends, but doesn't specify a name
   * that is found in their list of friends.
   */
  @Test
  public void testFriendLikesIntentInvalidName() throws Exception {
    bookTester.setParameters(
        "Show me Jimmy John's likes",
        "{\"friend\" : {\"name\": \"Jimmy John\"}}",
        "friendlikes",
        "authUser1",
        "authUser1@gmail.com");
    assertEquals(
        "I'm sorry. I don't recognize a Jimmy John in your contact list.",
        bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a successful friendlikes intent
   * when a user requests to see the liked books of one of their friends and their friends has
   * stored likes.
   */
  @Test
  public void testSuccessfulFriendLikesIntent() throws Exception {
    bookTester.setParameters(
        "Show me Jim Jones's liked books",
        "{\"friend\" : {\"name\": \"jim jones's\"}}",
        "friendlikes",
        "authUser1",
        "authUser1@gmail.com");

    // Jim Jones likes first and sixth book, reset order of sixth book to 1 (first index in this
    // list)
    sixthBook.setOrder(1);
    ArrayList<Book> expectedLikes = new ArrayList<Book>(Arrays.asList(firstBook, sixthBook));

    assertEquals("Here are Jim Jones's liked books.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedLikes), bookTester.getDisplay());
    assertEquals("query-2", bookTester.getRedirect());
  }

  /**
   * This function tests the output, fulfillment, and redirect for a friendlikes intent when a user
   * requests to see the liked books of one of their friends, but the friend hasn't liked any books.
   */
  @Test
  public void testEmptyFriendLikesIntent() throws Exception {
    bookTester.setParameters(
        "Show me Claire Crown's liked books",
        "{\"friend\" : {\"name\": \"claire crown's\"}}",
        "friendlikes",
        "authUser1",
        "authUser1@gmail.com");

    assertEquals("I couldn't find any liked books for Claire Crown.", bookTester.getFulfillment());
    assertNull(bookTester.getDisplay());
    assertNull(bookTester.getRedirect());
  }

  /**
   * Checks whether the output, display, and redirect when the user requests to see more books in
   * one of their bookshelves
   */
  @Test
  public void testNextPageOfBookshelfRequest() throws Exception {
    bookTester.setParameters(
        "Next page of my favorites bookshelf",
        "{}",
        "more",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    ArrayList<Book> expectedList = new ArrayList<Book>();
    for (int i = 5; i < bookshelfBooks.size(); ++i) {
      expectedList.add(bookshelfBooks.get(i));
    }
    assertEquals("Here's the next page of your Favorites bookshelf.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedList), bookTester.getDisplay());
    assertEquals("query-1-shelf", bookTester.getRedirect());
  }

  /**
   * Checks whether the output, display, and redirect when the user requests to see more books in
   * one of their bookshelves, but there are no more books to show.
   */
  @Test
  public void testLastPageOfBookshelfRequest() throws Exception {
    bookTester.setParameters(
        "Next page of my favorites bookshelf",
        "{}",
        "more",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    bookTester.setParameters(
        "Next page of my favorites bookshelf",
        "{}",
        "more",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    ArrayList<Book> expectedList = new ArrayList<Book>();
    for (int i = 5; i < bookshelfBooks.size(); ++i) {
      expectedList.add(bookshelfBooks.get(i));
    }
    assertEquals("This is the last page of your Favorites bookshelf.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedList), bookTester.getDisplay());
    assertEquals("query-1-shelf", bookTester.getRedirect());
  }

  /**
   * Checks whether the output, display, and redirect when the user requests to see the previous
   * page of books in one of their bookshelves.
   */
  @Test
  public void testPreviousPageOfBookshelfRequest() throws Exception {
    bookTester.setParameters(
        "Next page of my favorites bookshelf",
        "{}",
        "more",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    bookTester.setParameters(
        "Previous page of my favorites bookshelf",
        "{}",
        "previous",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    ArrayList<Book> expectedList = new ArrayList<Book>();
    for (int i = 0; i < 5; ++i) {
      expectedList.add(bookshelfBooks.get(i));
    }
    assertEquals(
        "Here's the previous page of your Favorites bookshelf.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedList), bookTester.getDisplay());
    assertEquals("query-1-shelf", bookTester.getRedirect());
  }

  /**
   * Checks whether the output, display, and redirect when the user requests to see the previous
   * page of books in one of their bookshelves, but they are seeing the first page.
   */
  @Test
  public void testFirstPageOfBookshelfRequest() throws Exception {
    bookTester.setParameters(
        "Previous page of my favorites bookshelf",
        "{}",
        "previous",
        "authUser1",
        "query-1-shelf",
        "authUser1@gmail.com");
    ArrayList<Book> expectedList = new ArrayList<Book>();
    for (int i = 0; i < 5; ++i) {
      expectedList.add(bookshelfBooks.get(i));
    }
    assertEquals(
        "This is the first page of your Favorites bookshelf.", bookTester.getFulfillment());
    assertEquals(BooksAgentHelper.listToJson(expectedList), bookTester.getDisplay());
    assertEquals("query-1-shelf", bookTester.getRedirect());
  }

  private ArrayList<Friend> getFriendList() {
    ArrayList<Friend> friends = new ArrayList<Friend>();
    friends.add(new Friend("John Doe", new ArrayList<String>(Arrays.asList("johndoe@gmail.com"))));
    friends.add(new Friend("Jane Smith", new ArrayList<String>(Arrays.asList("jane@gmail.com"))));
    friends.add(new Friend("James Ray", new ArrayList<String>(Arrays.asList("james@gmail.com"))));
    friends.add(
        new Friend(
            "Jim Jones",
            new ArrayList<String>(Arrays.asList("jimbo@gmail.com", "jimmy1@gmail.com"))));
    friends.add(new Friend("Mary Ann", new ArrayList<String>(Arrays.asList("maryann@gmail.com"))));
    friends.add(
        new Friend("Claire Crown", new ArrayList<String>(Arrays.asList("clairec@gmail.com"))));
    return friends;
  }

  private void setFriendsLikedBooks() {
    this.firstBook = new Book("Dear John", "0", 0);
    this.secondBook = new Book("Greece", "1", 1);
    this.thirdBook = new Book("Love Actually", "2", 2);
    this.fourthBook = new Book("Saving Alaska", "3", 3);
    this.fifthBook = new Book("The Fault in our Stars", "4", 4);
    this.sixthBook = new Book("The Notebook", "5", 5);

    bookTester.setLikedBook(firstBook, "JohnDoe", "johndoe@gmail.com");
    bookTester.setLikedBook(firstBook, "JimJones", "jimbo@gmail.com");
    bookTester.setLikedBook(secondBook, "MaryAnn", "maryann@gmail.com");
    bookTester.setLikedBook(secondBook, "RandomPerson1", "notafriend@gmail.com");
    bookTester.setLikedBook(thirdBook, "RandomPerson2", "notafriend2@gmail.com");
    bookTester.setLikedBook(thirdBook, "JaneSmith", "jane@gmail.com");
    bookTester.setLikedBook(fourthBook, "JamesRay", "james@gmail.com");
    bookTester.setLikedBook(fifthBook, "JohnDoe", "johndoe@gmail.com");
    bookTester.setLikedBook(sixthBook, "JimJones", "jimmy1@gmail.com");
    bookTester.setLikedBook(sixthBook, "JimJones", "jimbo@gmail.com");
  }

  private void unlikeAllFriendsBooks() {
    bookTester.setUnlikedBook(firstBook, "JohnDoe", "johndoe@gmail.com");
    bookTester.setUnlikedBook(firstBook, "JimJones", "jimbo@gmail.com");
    bookTester.setUnlikedBook(secondBook, "MaryAnn", "maryann@gmail.com");
    bookTester.setUnlikedBook(thirdBook, "JaneSmith", "jane@gmail.com");
    bookTester.setUnlikedBook(fourthBook, "JamesRay", "james@gmail.com");
    bookTester.setUnlikedBook(fifthBook, "JohnDoe", "johndoe@gmail.com");
    bookTester.setUnlikedBook(sixthBook, "JimJones", "jimmy1@gmail.com");
    bookTester.setUnlikedBook(sixthBook, "JimJones", "jimbo@gmail.com");
  }

  private void clearDatabase() {
    bookTester.deleteFromCustomDatabase("unloggedInUser");
    bookTester.deleteFromCustomDatabase("unAuthenticatedUser");
    bookTester.deleteFromCustomDatabase("authUser1");
    bookTester.deleteFromCustomDatabase("fallbackTestingID");
  }
}
