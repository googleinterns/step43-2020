package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
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
  private String userID;
  private ArrayList<String> shelvesNames;
  private ArrayList<String> lowerShelvesNames;

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
      handleSearchIntent(parameters);
    } else if (intentName.equals("more")) {
      handleMoreIntent();
    } else if (intentName.equals("previous")) {
      handlePreviousIntent();
    } else if (intentName.equals("description") || intentName.equals("preview")) {
      handleBookInfoIntents(parameters);
    } else if (intentName.equals("results")) {
      handleResultsIntent();
    } else {
      // All other intents require users to be logged in
      if (!userService.isUserLoggedIn()) {
        this.output = "Please login first.";
        return;
      }
      this.userID = userService.getCurrentUser().getUserId();
      if (!hasBookAuthentication(userID)) {
        // Get valid authentication
        this.output = "Please allow me to access your Google Books account first.";
        this.redirect =
            "https://8080-fabf4299-6bc0-403a-9371-600927588310.us-west1.cloudshell.dev/oauth2";
        return;
      }

      // Retrieve stored or store bookshelf names for current user
      if (BooksMemoryUtils.hasBookshelvesStored(userID, datastore)) {
        this.shelvesNames = BooksMemoryUtils.getStoredBookshelfNames(userID, datastore);
      } else {
        this.shelvesNames = BookUtils.getBookshelvesNames(userID);
        BooksMemoryUtils.storeBookshelfNames(shelvesNames, userID, datastore);
      }
      this.lowerShelvesNames = allLowerCaseList(shelvesNames);

      if (intentName.equals("library")) {
        handleLibraryIntent(parameters);
      } else if (intentName.equals("add")) {
        handleAddIntent(parameters);
      } else if (intentName.equals("delete")) {
        handleDeleteIntent(parameters);
      } else if (intentName.equals("friends")) {
        // TODO: retrieve list of friends from PeopleUtils.getFriends
        // Retrieve books their friends have liked
        // Create display of books
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
   * Handles book search intents by: creating BookQuery, retrieving appropriate Books, and handling
   * a new query storage and display if the request is successful.
   *
   * @param parameters Map of parameters from Dialogflow
   */
  private void handleSearchIntent(Map<String, Value> parameters) throws IOException {
    this.query = BookQuery.createBookQuery(this.userInput, parameters);
    this.startIndex = 0;

    // Retrieve books from BookQuery
    this.bookResults = BookUtils.getRequestedBooks(query, startIndex);
    this.totalResults = BookUtils.getTotalVolumesFound(query, startIndex);
    this.resultsReturned = bookResults.size();

    if (resultsReturned > 0) {
      handleNewQuerySuccess("search");
      this.output = "Here's what I found.";
    } else {
      this.output = "I couldn't find any results. Can you try again?";
    }
  }

  /**
   * Handles book more intents by: loading previous BookQuery, determining whether more books need
   * to be retrieved from Google Books API based on the next start index, and setting display to
   * next page of results if appropriate.
   */
  private void handleMoreIntent() throws IOException {
    // Loads query, prevStartIndex totalResults, resultsStored and increment startIndex
    loadBookQueryInfo(sessionID, queryID);
    this.startIndex = getNextStartIndex(prevStartIndex, totalResults);
    String ending = getFulfillmentEnding(query);

    if (startIndex == -1) {
      this.output = "This is the last page of " + ending;
      this.startIndex = prevStartIndex;
    } else if (nextPageOfBooksIsStored()) {
      replaceIndices(sessionID, queryID);
      this.output = "Here's the next page of " + ending;
    } else {
      // Make public book search from stored query parameters at startIndex
      if (!query.isMyLibrary()) {
        this.bookResults = BookUtils.getRequestedBooks(query, startIndex);
      } else {
        // Make private bookshelf search for userID from query parameters at startIndex
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
        this.output = "This is the last page of " + ending;
        this.startIndex = prevStartIndex;
      } else {
        // Store Book results and new indices
        BooksMemoryUtils.storeBooks(bookResults, startIndex, sessionID, queryID, datastore);
        replaceIndices(sessionID, queryID);
        this.output = "Here's the next page of " + ending;
      }
    }
    setBookListDisplay();
    this.redirect = queryID;
  }

  /**
   * Handles previous intents by: loading previous BookQuery, retrieving the previously displayed
   * stored Books and setting display.
   */
  private void handlePreviousIntent() throws IOException {
    // Loads query, prevStartIndex totalResults, resultsStored and increment startIndex
    loadBookQueryInfo(sessionID, queryID);
    this.startIndex = prevStartIndex - displayNum;
    String ending = getFulfillmentEnding(query);

    if (startIndex <= -1) {
      this.output = "This is the first page of " + ending;
      startIndex = 0;
    } else {
      replaceIndices(sessionID, queryID);
      this.output = "Here's the previous page of " + ending;
    }
    setBookListDisplay();
    this.redirect = queryID;
  }

  /**
   * Handles preview and description intents by: retrieving requested Book and setting display
   * output.
   *
   * @param parameters Map of parameters from Dialogflow
   */
  private void handleBookInfoIntents(Map<String, Value> parameters) throws IOException {
    int bookNumber = (int) parameters.get("number").getNumberValue();

    this.prevStartIndex =
        BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID, datastore);
    Book requestedBook =
        BooksMemoryUtils.getBookFromOrderNum(
            bookNumber, prevStartIndex, sessionID, queryID, datastore);

    setSingleBookDisplay(requestedBook);
    this.redirect = queryID;
    this.output = "Here's a " + intentName + " of " + requestedBook.getTitle() + ".";
  }

  /**
   * Handles results intent by: loading previous BookQuery for sessionID and queryID, refreshing
   * bookshelf is necessary, and setting display for stored book objects from previous start index.
   */
  private void handleResultsIntent() throws IOException {
    // Loads query, prevStartIndex totalResults, resultsStored and increment startIndex
    loadBookQueryInfo(sessionID, queryID);
    if (query.isMyLibrary()) {
      this.userID = userService.getCurrentUser().getUserId();
      refreshBookshelf();
      if (resultsReturned == 0) {
        this.output = "There are no more books in your " + query.getBookshelfName() + " bookshelf.";
      } else {
        setBookListDisplay();
        this.output = "Here's your " + query.getBookshelfName() + " bookshelf.";
      }
    } else {
      this.startIndex = prevStartIndex;
      setBookListDisplay();
      this.output = "Here are the results.";
    }
    this.redirect = queryID;
  }

  /**
   * Handles library intent by: retrieving valid bookshelf name from parameters, creating book query
   * object if parameters are valid and retrieving books from bookshelf in user's library. If
   * retrieval is successful, sets display to retrieved books.
   *
   * @param parameters Map of parameters from Dialogflow
   */
  private void handleLibraryIntent(Map<String, Value> parameters) throws IOException {
    // Check for valid bookshelf parameter
    if (parameters.get("bookshelf") == null
        || !lowerShelvesNames.contains(
            parameters.get("bookshelf").getStringValue().toLowerCase())) {
      ArrayList<String> displayNames = BookUtils.getBookshelvesNames(userID);
      this.output = "Which bookshelf would you like to see?";
      this.display = listToJson(shelvesNames);
      this.redirect = "bookshelf-names";
      return;
    }
    // Create BookQuery
    this.query = BookQuery.createBookQuery(userInput, parameters, true);
    this.startIndex = 0;

    // Retrieve books from BookQuery
    this.bookResults = BookUtils.getBookShelfBooks(query, startIndex, userID);
    this.totalResults = BookUtils.getTotalShelfVolumesFound(query, startIndex, userID);
    this.resultsReturned = bookResults.size();

    if (resultsReturned > 0) {
      handleNewQuerySuccess("library");
      this.output = "Here are the books in your " + query.getBookshelfName() + " bookshelf.";
    } else {
      this.output = "There are no books in your " + query.getBookshelfName() + " bookshelf.";
    }
  }

  /**
   * Handles add intent by: retrieving requested book to add to bookshelf, retrieving bookshelf
   * name, and adding book to bookshelf. If successful, sets display to the Book that was added.
   *
   * @param parameters Map of parameters from Dialogflow
   */
  private void handleAddIntent(Map<String, Value> parameters) throws IOException {
    int bookNumber = (int) parameters.get("number").getNumberValue();
    this.prevStartIndex =
        BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID, datastore);
    Book requestedBook =
        BooksMemoryUtils.getBookFromOrderNum(
            bookNumber, prevStartIndex, sessionID, queryID, datastore);

    // Check for valid bookshelf parameter
    if (parameters.get("bookshelf") == null
        || !checkNames.contains(parameters.get("bookshelf").getStringValue().toLowerCase())) {
      ArrayList<String> displayNames = BookUtils.getBookshelvesNames(userID);
      this.output = "Which bookshelf would you like to add " + requestedBook.getTitle() + " to?";
      this.display = listToJson(getValidAddShelves(shelvesNames, requestedBook));
      return;
    }
    String bookshelfName = parameters.get("bookshelf").getStringValue();
    String volumeId = requestedBook.getVolumeId();
    try {
      BookUtils.addToBookshelf(bookshelfName, userID, volumeId);
      this.output =
          "I've added " + requestedBook.getTitle() + " to your " + bookshelfName + " bookshelf.";
      setSingleBookDisplay(requestedBook);
      this.redirect = queryID;
    } catch (GoogleJsonResponseException e) {
      this.output =
          "I'm sorry. I couldn't add "
              + requestedBook.getTitle()
              + " to your "
              + bookshelfName
              + " bookshelf.";
    }
  }

  /**
   * Handles delete intent by: retrieving requested book to delete from bookshelf, deleting the book
   * from the user's bookshelf. If successful, sets display to the Book that was deleted.
   *
   * @param parameters Map of parameters from Dialogflow
   */
  private void handleDeleteIntent(Map<String, Value> parameters) throws IOException {
    // Load bookshelf name from stored BookQuery
    loadBookQueryInfo(sessionID, queryID);
    String shelfName = query.getBookshelfName();
    // Retrieve requested book
    int bookNumber = (int) parameters.get("number").getNumberValue();
    this.prevStartIndex =
        BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID, datastore);
    Book requestedBook =
        BooksMemoryUtils.getBookFromOrderNum(
            bookNumber, prevStartIndex, sessionID, queryID, datastore);
    String volumeId = requestedBook.getVolumeId();
    try {
      BookUtils.deleteFromBookshelf(shelfName, userID, volumeId);
      this.output =
          "I've deleted " + requestedBook.getTitle() + " from your " + shelfName + " bookshelf.";
      setSingleBookDisplay(requestedBook);
      this.redirect = queryID;
    } catch (GoogleJsonResponseException e) {
      this.output =
          "I'm sorry. I couldn't delete "
              + requestedBook.getTitle()
              + " from your "
              + shelfName
              + " bookshelf.";
    }
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
   *
   * @param intent detected intent for query
   */
  private void handleNewQuerySuccess(String intent) {
    this.queryID = getNextQueryID(sessionID);
    if (intent.equals("library")) {
      this.queryID += "-shelf";
    }
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
    this.display = listToJson(booksToDisplay);
  }

  /**
   * Sets display to Book specified in parameters and assigns like status to display on interface
   */
  private void setSingleBookDisplay(Book book) {
    Book bookToDisplay = BooksMemoryUtils.assignLikeStatus(book, sessionID, datastore);
    this.display = bookToJson(bookToDisplay);
  }

  /**
   * Replaces stored books and indices information in datastore for a user's bookshelf after the
   * user edited the contents of the bookshelf via the interface.
   */
  private void refreshBookshelf() throws IOException {
    this.startIndex = 0;
    this.bookResults = BookUtils.getBookShelfBooks(query, startIndex, userID);
    this.totalResults = BookUtils.getTotalShelfVolumesFound(query, startIndex, userID);
    this.resultsReturned = bookResults.size();
    BooksMemoryUtils.deleteStoredEntities("Book", sessionID, queryID, datastore);
    BooksMemoryUtils.storeBooks(bookResults, startIndex, sessionID, queryID, datastore);
    replaceIndices(sessionID, queryID);
  }

  /**
   * Replaces previous Indices Entity stored in Datastore with the new startIndex, totalResults,
   * resultsStored, displayNum for the Indices Entity that matches the current sessionID and queryID
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

  private ArrayList<String> allLowerCaseList(ArrayList<String> list) {
    ArrayList<String> lowerCaseList = new ArrayList<String>();
    for (String word : list) {
      lowerCaseList.add(word.toLowerCase());
    }
    return lowerCaseList;
  }

  /**
   * Returns a list of valid shelves to add Book object to, based on type of book and access type of
   * generic Google Books shelves
   *
   * @param shelves list of all user's shelves
   * @param bookToAdd Book to add
   * @return ArrayList<String> valid shelves
   */
  private static ArrayList<String> getValidAddShelves(ArrayList<String> shelves, Book bookToAdd) {
    shelves.remove("Purchased");
    shelves.remove("Reviewed");
    shelves.remove("Recently viewed");
    shelves.remove("Browsing history");
    shelves.remove("Books for you");
    if (!bookToAdd.isEbook()) {
      shelves.remove("My Google eBooks");
    }
    return shelves;
  }

  public static String listToJson(ArrayList<?> list) {
    Gson gson = new Gson();
    return gson.toJson(list);
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

  private String getFulfillmentEnding(BookQuery query) {
    String ending = "";
    if (query.isMyLibrary()) {
      ending = " your " + query.getBookshelfName() + " bookshelf.";
    } else {
      ending = "results.";
    }
    return ending;
  }

  private boolean nextPageOfBooksIsStored() {
    return (startIndex + displayNum <= resultsStored || startIndex + displayNum >= totalResults);
  }
}
