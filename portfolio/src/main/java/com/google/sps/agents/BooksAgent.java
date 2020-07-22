package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.users.UserService;
import com.google.gson.Gson;
import com.google.protobuf.Value;
import com.google.sps.data.Book;
import com.google.sps.data.BookQuery;
import com.google.sps.utils.BookUtils;
import com.google.sps.utils.BooksMemoryUtils;
import com.google.sps.utils.OAuthHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Books Agent handles user's requests for books from Google Books API. It determines appropriate
 * outputs and display information to send to the user interface based on Dialogflow's detected Book
 * intent.
 */
public class BooksAgent implements Agent {
  private final String intentName;
  private final String userInput;
  private String output;
  private String display;
  private String redirect;

  private int displayNum;
  private String sessionID;
  private String queryID;
  private DatastoreService datastore;
  private UserService userService;

  private BookQuery query;
  private ArrayList<Book> bookResults;
  private int startIndex;
  private int totalResults;
  private int resultsReturned;
  private int prevStartIndex;
  private int resultsStored;

  /**
   * BooksAgent constructor without queryID sets queryID property to the most recent queryID for the
   * specified sessionID
   *
   * @param intentName String containing the specific intent within memory agent that user is
   *     requesting.
   * @param userInput String containing user's request input
   * @param parameters Map containing the detected entities in the user's intent.
   * @param sessionID String containing the unique sessionID for user's session
   * @param userService UserService instance to access userID and other user info.
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public BooksAgent(
      String intentName,
      String userInput,
      Map<String, Value> parameters,
      String sessionID,
      UserService userService,
      DatastoreService datastore)
      throws IOException, IllegalArgumentException {
    this(intentName, userInput, parameters, sessionID, userService, datastore, null);
  }

  /**
   * BooksAgent constructor with queryID
   *
   * @param intentName String containing the specific intent within memory agent that user is
   *     requesting.
   * @param userInput String containing user's request input
   * @param parameters Map containing the detected entities in the user's intent.
   * @param sessionID String containing the unique sessionID for user's session
   * @param userService UserService instance to access userID and other user info.
   * @param datastore DatastoreService instance used to access book info grom database.
   * @param queryID String containing the unique ID for the BookQuery the user is requesting, If
   *     request comes from Book Display interface, then queryID is retrieved from Book Display
   *     Otherwise, queryID is set to the most recent query that the user (sessionID) made.
   */
  public BooksAgent(
      String intentName,
      String userInput,
      Map<String, Value> parameters,
      String sessionID,
      UserService userService,
      DatastoreService datastore,
      String queryID)
      throws IOException, IllegalArgumentException {
    this.displayNum = 5;
    this.intentName = intentName;
    this.userInput = userInput;
    this.sessionID = sessionID;
    this.userService = userService;
    this.datastore = datastore;
    this.queryID = queryID;
    if (queryID == null) {
      this.queryID = getMostRecentQueryID(sessionID);
    }
    setParameters(parameters);
  }

  @Override
  public void setParameters(Map<String, Value> parameters)
      throws IOException, IllegalArgumentException {
    // Intents that do not require user to be authenticated
    if (intentName.equals("search")) {
      this.query = BookQuery.createBookQuery(this.userInput, parameters);
      this.startIndex = 0;

      // Retrieve books from BookQuery
      this.bookResults = BookUtils.getRequestedBooks(query, startIndex);
      this.totalResults = BookUtils.getTotalVolumesFound(query, startIndex);
      this.resultsReturned = bookResults.size();

      if (resultsReturned > 0) {
        handleNewQuerySuccess();
        this.output = "Here's what I found.";
      } else {
        this.output = "I couldn't find any results. Can you try again?";
      }

    } else if (intentName.equals("more")) {
      // Load BookQuery, totalResults, resultsStored and increment startIndex
      loadBookQueryInfo(sessionID, queryID);
      this.startIndex = getNextStartIndex(prevStartIndex, totalResults);

      if (startIndex == -1) {
        this.output = "This is the last page of results.";
        this.startIndex = prevStartIndex;
      } else if (startIndex + displayNum <= resultsStored
          || startIndex + displayNum >= totalResults) {
        replaceIndices(sessionID, queryID);
        this.output = "Here's the next page of results.";
      } else {
        // Make public book search from stored query parameters at startIndex
        if (!query.isMyLibrary()) {
          this.bookResults = BookUtils.getRequestedBooks(query, startIndex);
        } else {
          // Make book search from Bookshelf
          if (!userService.isUserLoggedIn()) {
            this.output = "Please login first.";
            return;
          }
          String userID = userService.getCurrentUser().getUserId();
          this.bookResults = BookUtils.getBookShelfBooks(query, startIndex, userID);
        }
        int resultsReturned = bookResults.size();
        int newResultsStored = resultsReturned + resultsStored;
        this.resultsStored = newResultsStored;

        if (resultsReturned == 0) {
          this.output = "This is the last page of results.";
          this.startIndex = prevStartIndex;
        } else {
          // Store Book results and new indices
          BooksMemoryUtils.storeBooks(bookResults, startIndex, sessionID, queryID, datastore);
          replaceIndices(sessionID, queryID);
          this.output = "Here's the next page of results.";
        }
      }
      setBookListDisplay();
      this.redirect = queryID;

    } else if (intentName.equals("previous")) {
      loadBookQueryInfo(sessionID, queryID);
      this.startIndex = prevStartIndex - displayNum;

      if (startIndex <= -1) {
        this.output = "This is the first page of results.";
        startIndex = 0;
      } else {
        replaceIndices(sessionID, queryID);
        this.output = "Here's the previous page of results.";
      }
      setBookListDisplay();
      this.redirect = queryID;

    } else if (intentName.equals("description") || intentName.equals("preview")) {
      int bookNumber = (int) parameters.get("number").getNumberValue();

      this.prevStartIndex =
          BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID, datastore);
      Book requestedBook =
          BooksMemoryUtils.getBookFromOrderNum(
              bookNumber, prevStartIndex, sessionID, queryID, datastore);

      this.display = bookToJson(requestedBook);
      this.redirect = queryID;
      this.output = "Here's a " + intentName + " of " + requestedBook.getTitle() + ".";

    } else if (intentName.equals("results")) {
      loadBookQueryInfo(sessionID, queryID);
      this.startIndex = prevStartIndex;
      setBookListDisplay();
      if (query.isMyLibrary()) {
        this.output = "Here's your " + query.getBookshelfName() + " bookshelf.";
      } else {
        this.output = "Here are the results.";
      }
      this.redirect = queryID;
    } else {
      // All other intents require users to be logged in
      if (!userService.isUserLoggedIn()) {
        this.output = "Please login first.";
        return;
      }
      String userID = userService.getCurrentUser().getUserId();

      if (intentName.equals("library")) {
        if (!hasBookAuthentication(userID)) {
          // Get valid authentication
          this.output = "Please allow me to access your Google Books account first.";
          this.redirect =
              "https://8080-fabf4299-6bc0-403a-9371-600927588310.us-west1.cloudshell.dev/oauth2";
          return;
        }
        ArrayList<String> shelvesNames = BookUtils.getBookshelvesNames(userID);
        ArrayList<String> checkNames = allLowerCaseList(shelvesNames);

        // If unspecified bookshelf, or invalid bookshelf name
        if (parameters.get("bookshelf") == null
            || !checkNames.contains(parameters.get("bookshelf").getStringValue().toLowerCase())) {
          ArrayList<String> displayNames = BookUtils.getBookshelvesNames(userID);
          this.output = "Which bookshelf would you like to see?";
          this.display = listToString(shelvesNames);
          return;
        } else {
          // Create BookQuery
          this.query = BookQuery.createBookQuery(this.userInput, parameters);
          this.startIndex = 0;

          // Retrieve books from BookQuery
          this.bookResults = BookUtils.getBookShelfBooks(query, startIndex, userID);
          this.totalResults = BookUtils.getTotalShelfVolumesFound(query, startIndex, userID);
          this.resultsReturned = bookResults.size();

          if (resultsReturned > 0) {
            handleNewQuerySuccess();
            this.output = "Here are the books in your " + query.getBookshelfName() + " bookshelf.";
          } else {
            this.output = "There are no books in your " + query.getBookshelfName() + " bookshelf.";
          }
        }
      }
    }
  }

  @Override
  public String getOutput() {
    return this.output;
  }

  @Override
  public String getDisplay() {
    return this.display;
  }

  @Override
  public String getRedirect() {
    return this.redirect;
  }

  /**
   * This function determines if the current user has stored book credentials
   *
   * @param userID ID of current user logged in
   * @return boolean indicating if user has book credentials
   */
  private boolean hasBookAuthentication(String userID) throws IOException {
    OAuthHelper helper = new OAuthHelper();
    return (helper.loadUserCredential(userID) != null);
  }

  /**
   * Upon a successful new book query request, triggered by books.search and books.library intents,
   * this function does the following:
   *
   * <p>Retrieves the next unique queryID, stores BookQuery, Book results, and Indices for the
   * corresponding sessionID and queryID of the new query. Retrieves the list of books to display.
   * Sets the display and queryID redirect.
   */
  private void handleNewQuerySuccess() {
    this.queryID = getNextQueryID(sessionID);

    // Store BookQuery, Book results, totalResults, resultsReturned
    BooksMemoryUtils.storeBooks(bookResults, startIndex, sessionID, queryID, datastore);
    BooksMemoryUtils.storeBookQuery(query, sessionID, queryID, datastore);
    BooksMemoryUtils.storeIndices(
        startIndex, totalResults, resultsReturned, displayNum, sessionID, queryID, datastore);

    setBookListDisplay();
    this.redirect = queryID;
  }

  /**
   * Loads previous BookQuery and Indices information from the BookQuery matching the request's
   * sessionID and queryID and sets query, prevStartIndex, resultsStored, totalResults based on
   * stored information
   *
   * @param sessionID ID of current user / session
   * @param queryID ID of query requested
   */
  private void loadBookQueryInfo(String sessionID, String queryID) {
    this.query = BooksMemoryUtils.getStoredBookQuery(sessionID, queryID, datastore);
    this.prevStartIndex =
        BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID, datastore);
    this.resultsStored =
        BooksMemoryUtils.getStoredIndices("resultsStored", sessionID, queryID, datastore);
    this.totalResults =
        BooksMemoryUtils.getStoredIndices("totalResults", sessionID, queryID, datastore);
  }

  /** Sets display to an ArrayList<Book> to display on user interface */
  private void setBookListDisplay() {
    ArrayList<Book> booksToDisplay =
        BooksMemoryUtils.getStoredBooksToDisplay(
            displayNum, startIndex, sessionID, queryID, datastore);
    this.display = bookListToJson(booksToDisplay);
  }

  /**
   * Replaces previous Indices Entity stored in Datastore with the new startIndex, totalResults,
   * resultsSTored, displayNum for the Indices Entity that matches the current sessionID and queryID
   * for the request
   *
   * @param sessionID ID of current user / session
   * @param queryID ID of query requested
   */
  private void replaceIndices(String sessionID, String queryID) {
    BooksMemoryUtils.deleteStoredEntities("Indices", sessionID, queryID, datastore);
    BooksMemoryUtils.storeIndices(
        startIndex, totalResults, resultsStored, displayNum, sessionID, queryID, datastore);
  }

  private int getNextStartIndex(int prevIndex, int total) {
    int nextIndex = prevIndex + displayNum;
    if (nextIndex < total) {
      return nextIndex;
    }
    return -1;
  }

  public static String bookToJson(Book book) {
    Gson gson = new Gson();
    return gson.toJson(book);
  }

<<<<<<< HEAD
  public static String bookListToJson(ArrayList<Book> books) {
=======
  private ArrayList<String> allLowerCaseList(ArrayList<String> list) {
    ArrayList<String> lowerCaseList = new ArrayList<String>();
    for (String word : list) {
      lowerCaseList.add(word.toLowerCase());
    }
    return lowerCaseList;
  }

  private String listToString(ArrayList<String> list) {
    Gson gson = new Gson();
    return gson.toJson(list);
  }

  private String bookListToString(ArrayList<Book> books) {
>>>>>>> Add handling to support MyLibrary calls for authorized users
    Gson gson = new Gson();
    return gson.toJson(books);
  }

  private String getMostRecentQueryID(String sessionID) {
    int queryNum = BooksMemoryUtils.getNumQueryStored(sessionID, datastore);
    String queryID = "query-" + Integer.toString(queryNum);
    return queryID;
  }

  private String getNextQueryID(String sessionID) {
    int queryNum = BooksMemoryUtils.getNumQueryStored(sessionID, datastore) + 1;
    String queryID = "query-" + Integer.toString(queryNum);
    return queryID;
  }
}
