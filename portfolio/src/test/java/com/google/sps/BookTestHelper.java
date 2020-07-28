package com.google.sps.servlets;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.testing.json.GoogleJsonResponseExceptionFactoryTesting;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.testing.json.MockJsonFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.sps.agents.BooksAgent;
import com.google.sps.data.Book;
import com.google.sps.data.BookQuery;
import com.google.sps.data.Friend;
import com.google.sps.utils.BookUtils;
import com.google.sps.utils.BooksMemoryUtils;
import com.google.sps.utils.OAuthHelper;
import com.google.sps.utils.PeopleUtils;
import java.io.IOException;
import java.util.ArrayList;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookTestHelper {
  private static Logger log = LoggerFactory.getLogger(BookTestHelper.class);
  @Mock UserService userServiceMock;
  @Mock OAuthHelper oauthHelperMock;
  @Mock PeopleUtils peopleUtilsMock;
  @Mock BookUtils bookUtilsMock;

  BooksAgent booksAgent;
  DatastoreService customDatastore;
  String sessionId;
  String queryId;

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  /** Default constructor for BookTestHelper that sets up testing environment and empty mocks. */
  public BookTestHelper() {
    helper.setUp();
    customDatastore = DatastoreServiceFactory.getDatastoreService();
    userServiceMock = mock(UserService.class);
    oauthHelperMock = mock(OAuthHelper.class);
    peopleUtilsMock = mock(PeopleUtils.class);
    bookUtilsMock = mock(BookUtils.class);
    this.sessionId = "fallbackTestingID";
    this.queryId = null;
  }

  /**
   * Sets the general return parameters to mock OAuthHelper when userEmail and queryId are not
   * specified, i.e. for an un-authenticated query request
   *
   * @param inputText Text input of user's intent sent to Dialogflow.
   * @param parameters String json that consists of expected parameters identified by Dialogflow.
   * @param intentName String name of expected intent identified by Dialogflow.
   * @param sessionId String unique ID for current session
   */
  public void setParameters(
      String inputText, String parameters, String intentName, String sessionId)
      throws InvalidProtocolBufferException, IOException {
    setParameters(inputText, parameters, intentName, sessionId, null, null);
  }

  /**
   * Sets the general return parameters to mock OAuthHelper when queryId is not specified, i.e. for
   * a new query request
   *
   * @param inputText Text input of user's intent sent to Dialogflow.
   * @param parameters String json that consists of expected parameters identified by Dialogflow.
   * @param intentName String name of expected intent identified by Dialogflow.
   * @param sessionId String unique ID for current session
   * @param userEmail email associated with user ID
   */
  public void setParameters(
      String inputText, String parameters, String intentName, String sessionId, String userEmail)
      throws InvalidProtocolBufferException, IOException {
    setParameters(inputText, parameters, intentName, sessionId, null, userEmail);
  }

  /**
   * Sets the parameters for BookAgent instance.
   *
   * @param inputText Text input of user's intent sent to Dialogflow.
   * @param parameters String json that consists of expected parameters identified by Dialogflow.
   * @param intentName String name of expected intent identified by Dialogflow.
   * @param sessionId String unique ID for current session
   * @param queryId String unique ID for current query
   * @param userEmail email associated with user ID
   */
  public void setParameters(
      String inputText,
      String parameters,
      String intentName,
      String sessionId,
      String queryId,
      String userEmail)
      throws InvalidProtocolBufferException, IOException {
    this.sessionId = sessionId;
    this.queryId = queryId;
    booksAgent =
        new BooksAgent(
            intentName,
            inputText,
            BookAgentServlet.stringToMap(parameters),
            sessionId,
            userServiceMock,
            customDatastore,
            queryId,
            oauthHelperMock,
            bookUtilsMock,
            peopleUtilsMock);
  }

  public String getFulfillment() {
    return booksAgent.getOutput();
  }

  public String getDisplay() {
    return booksAgent.getDisplay();
  }

  public String getRedirect() {
    return booksAgent.getRedirect();
  }

  /**
   * Sets list of Friend objects for a user returned from PeopleUtils mock when a user requests
   * their friends' likes and sets hasFriend to return true if the name is included in the list of
   * Friend objects. Otherwise, hasFriend will return false.
   *
   * @param userId unique Id of user
   * @param friends list of Friend objects
   */
  public void setFriends(String userID, ArrayList<Friend> friends) throws IOException {
    when(peopleUtilsMock.getFriends(eq(userID), any(OAuthHelper.class))).thenReturn(friends);
    when(peopleUtilsMock.hasFriend(eq(userID), any(String.class), any(OAuthHelper.class)))
        .thenReturn(false);
    for (Friend friend : friends) {
      when(peopleUtilsMock.hasFriend(eq(userID), eq(friend.getName()), any(OAuthHelper.class)))
          .thenReturn(true);
    }
  }

  /**
   * Sets the list of bookshelf names returned from the BookUtils mock when a user requests
   * bookshelves from their libraries
   *
   * @param userId unique Id of user
   * @param bookshelfNames list of Bookshelf names
   */
  public void setBookshelfNames(String userId, ArrayList<String> bookshelfNames)
      throws IOException {
    when(bookUtilsMock.getBookshelvesNames(eq(userId), any(OAuthHelper.class)))
        .thenReturn(bookshelfNames);
  }

  /**
   * Sets BookUtils mock to edit the users bookshelf (add or delete), or fail to add the book to the
   * user's bookshelf and throw a GoogleJsonResponseException
   *
   * @param bookshelfName name of bookshelf to edit
   * @param userId unique Id of user
   * @param allowEditing boolean determining whether BookUtilsMock throws exception, indicating it
   *     could not edit user's bookshelf, or edits the user's bookshelf and returns nothing
   */
  public void setBookshelfEditingAccess(String bookshelfName, String userId, Boolean allowEditing)
      throws IOException {
    JsonFactory jsonFactory = new MockJsonFactory();
    GoogleJsonResponseException testException =
        GoogleJsonResponseExceptionFactoryTesting.newMock(
            jsonFactory, HttpStatusCodes.STATUS_CODE_FORBIDDEN, "Test Exception");
    if (allowEditing) {
      doNothing()
          .when(bookUtilsMock)
          .addToBookshelf(eq(bookshelfName), eq(userId), any(String.class), any(OAuthHelper.class));
      doNothing()
          .when(bookUtilsMock)
          .deleteFromBookshelf(
              eq(bookshelfName), eq(userId), any(String.class), any(OAuthHelper.class));
    } else {
      doThrow(testException)
          .when(bookUtilsMock)
          .addToBookshelf(eq(bookshelfName), eq(userId), any(String.class), any(OAuthHelper.class));
      doThrow(testException)
          .when(bookUtilsMock)
          .deleteFromBookshelf(
              eq(bookshelfName), eq(userId), any(String.class), any(OAuthHelper.class));
    }
  }

  /**
   * Sets the list of books returned from the BookUtils mock when a user requests books from their
   * authenticated bookshelf
   *
   * @param userId unique Id of user
   * @param bookshelfBooks list of Book objects
   * @param totalBooksFound number of books in certain bookshelf
   */
  public void setBookShelfBooks(String userId, ArrayList<Book> bookshelfBooks, int totalBooksFound)
      throws IOException {
    when(bookUtilsMock.getBookShelfBooks(
            any(BookQuery.class), anyInt(), eq(userId), any(OAuthHelper.class)))
        .thenReturn(bookshelfBooks);
    when(bookUtilsMock.getTotalShelfVolumesFound(
            any(BookQuery.class), anyInt(), eq(userId), any(OAuthHelper.class)))
        .thenReturn(totalBooksFound);
  }

  /**
   * Sets the list of books returned from the BookUtils mock when a user makes a generic book search
   *
   * @param books list of Book objects
   * @param totalBooksFound number of books in certain bookshelf
   */
  public void setSearchBooks(ArrayList<Book> books, int totalBooksFound) throws IOException {
    when(bookUtilsMock.getRequestedBooks(any(BookQuery.class), anyInt())).thenReturn(books);
    when(bookUtilsMock.getTotalVolumesFound(any(BookQuery.class), anyInt()))
        .thenReturn(totalBooksFound);
  }

  /**
   * Sets the mocks returned for a logged-in user with email and id from parameters
   *
   * @param email email of logged-in user
   * @param id id of logged-in user
   */
  public void setLoggedIn(String email, String id) {
    setUser(email, id);
  }

  /**
   * Sets the mocks returned for authenticated users
   *
   * @param id id of logged-in user
   */
  public void setAuthenticatedUser(String id) throws IOException {
    when(oauthHelperMock.hasBookAuthentication(id)).thenReturn(true);
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
   * This function adds a liked book for a user from the customDatastore.
   *
   * @param book book to like
   * @param userID unique id of user to add book for
   * @param userEmail unique email of user to add liked book for
   */
  public void setLikedBook(Book book, String userID, String userEmail) {
    BooksMemoryUtils.likeBook(book, userID, userEmail, customDatastore);
  }

  /**
   * This function deletes a liked book for a user from the customDatastore.
   *
   * @param book book to unlike
   * @param userID unique id of user to delete book for
   * @param userEmail unique email of user to delete liked book for
   */
  public void setUnlikedBook(Book book, String userID, String userEmail) {
    BooksMemoryUtils.unlikeBook(book, userID, userEmail, customDatastore);
  }

  /**
   * Populates customizes datastore with desired BookQuery object.
   *
   * @param query BookQuery object to store
   * @param sessionId unique id of session to store
   * @param queryId unique id (within sessionId) of query to store
   */
  public void setCustomDatabase(BookQuery query, String sessionId, String queryId) {
    BooksMemoryUtils.storeBookQuery(query, sessionId, queryId, customDatastore);
  }

  /**
   * Populates customizes datastore with desired Indices parameters, used to construct an indices
   * object.
   *
   * @param startIndex index to start retrieving Volume objects from
   * @param resultsStored number of results stored
   * @param totalResults total matches in Google Book API
   * @param displayNum number of results displayed request
   * @param sessionId unique id of session to store
   * @param queryId unique id (within sessionId) of query to store
   */
  public void setCustomDatabase(
      int startIndex,
      int totalResults,
      int resultsStored,
      int displayNum,
      String sessionId,
      String queryId) {
    BooksMemoryUtils.storeIndices(
        startIndex, totalResults, resultsStored, displayNum, sessionId, queryId, customDatastore);
  }

  /**
   * Populates customizes datastore with desired Books, starting at startIndex.
   *
   * @param books ArrayList of Book objects to store
   * @param startIndex index to start order at
   * @param sessionId unique id of session to store
   * @param queryId unique id (within sessionId) of query to store
   */
  public void setCustomDatabase(
      ArrayList<Book> books, int startIndex, String sessionId, String queryId) {
    BooksMemoryUtils.storeBooks(books, startIndex, sessionId, queryId, customDatastore);
  }

  /**
   * Clears all stored book information for sessionId from custom datastore.
   *
   * @param sessionId unique id of session to delete stored information from
   */
  public void deleteFromCustomDatabase(String sessionId) {
    BooksMemoryUtils.deleteAllStoredBookInformation(sessionId, customDatastore);
  }
}
