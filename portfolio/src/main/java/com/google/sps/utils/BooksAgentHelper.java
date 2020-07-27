package com.google.sps.utils;

// Imports the Google Cloud client library
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.users.UserService;
import com.google.gson.Gson;
import com.google.protobuf.Value;
import com.google.sps.data.Book;
import com.google.sps.data.BookQuery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * BooksAgentHelper handles user's requests for books from Google Books API. It determines
 * appropriate outputs and display information to send to the user interface based on Dialogflow's
 * detected Book intent.
 */
public class BooksAgentHelper {
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
  private String friendName;

  /**
   * BooksAgentHelper constructor
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
  public BooksAgentHelper(
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
  }

  public void handleAuthorizationIntents(Map<String, Value> parameters)
      throws IOException, IllegalArgumentException {
    if (!userService.isUserLoggedIn()) {
      this.output = "Please login first.";
      return;
    }
    this.userID = userService.getCurrentUser().getUserId();
    setBookshelfInfoForUser();

    if (intentName.equals("library")) {
      handleLibraryIntent(parameters);
    } else if (intentName.equals("add")) {
      handleAddIntent(parameters);
    } else if (intentName.equals("delete")) {
      handleDeleteIntent(parameters);
    } else if (intentName.equals("friends")
        || intentName.equals("mylikes")
        || intentName.equals("friendlikes")) {
      handleNewQueryIntents(intentName, parameters);
    }
  }

  public String getOutput() {
    return this.output;
  }

  public String getDisplay() {
    return this.display;
  }

  public String getRedirect() {
    return this.redirect;
  }

  /**
   * Retrieves bookshelf information for the logged in user and sets parameters accordingly,
   * including shelvesnames and lowerShelvesNames to be used for MyLibrary intents
   */
  private void setBookshelfInfoForUser() throws IOException, IllegalArgumentException {
    // Retrieve stored or store bookshelf names for current user
    if (BooksMemoryUtils.hasBookshelvesStored(userID, datastore)) {
      this.shelvesNames = BooksMemoryUtils.getStoredBookshelfNames(userID, datastore);
    } else {
      this.shelvesNames = BookUtils.getBookshelvesNames(userID);
      BooksMemoryUtils.storeBookshelfNames(shelvesNames, userID, datastore);
    }
    this.lowerShelvesNames = allLowerCaseList(shelvesNames);
  }
  /**
   * Creates new query request by: creating BookQuery, retrieving appropriate Books for the intent
   * and setting the totalResults and resultsReturned parameters
   *
   * @param intentName name of detected intent
   * @param parameters Map of parameters from Dialogflow
   */
  private void createNewQuery(String intentName, Map<String, Value> parameters) throws IOException {
    if (intentName.equals("library")) {
      this.query = BookQuery.createBookQuery(intentName, userInput, parameters, true);
    } else {
      this.query = BookQuery.createBookQuery(intentName, userInput, parameters);
    }
    this.startIndex = 0;

    if (intentName.equals("search")) {
      this.bookResults = BookUtils.getRequestedBooks(query, startIndex);
      this.totalResults = BookUtils.getTotalVolumesFound(query, startIndex);
    } else if (intentName.equals("library")) {
      this.bookResults = BookUtils.getBookShelfBooks(query, startIndex, userID);
      this.totalResults = BookUtils.getTotalShelfVolumesFound(query, startIndex, userID);
    } else {
      if (intentName.equals("friends")) {
        this.bookResults = BooksMemoryUtils.getFriendsLikes(userID, datastore);
      } else if (intentName.equals("mylikes")) {
        this.bookResults = BooksMemoryUtils.getLikedBooksFromId(userID, "id", datastore);
      } else if (intentName.equals("friendlikes")) {
        this.bookResults =
            BooksMemoryUtils.getLikesOfFriend(userID, query.getFriendName(), datastore);
      }
      this.totalResults = bookResults.size();
    }
    this.resultsReturned = bookResults.size();
  }

  /**
   * Handles new query Book intents that require retrieving new lists of books, based on user input.
   * Includes: search, library, friends, mylikes, friendlikes intents. Handles these intents by:
   * creating BookQuery, retrieving appropriate Books, and handling a new query storage and display
   * if the request is successful.
   *
   * @param intentName name of detected intent
   * @param parameters Map of parameters from Dialogflow
   */
  public void handleNewQueryIntents(String intentName, Map<String, Value> parameters)
      throws IOException {
    createNewQuery(intentName, parameters);
    this.startIndex = 0;
    if (intentName.equals("friendlikes")) {
      if (!isValidFriend()) {
        return;
      }
    }
    if (resultsReturned > 0) {
      handleNewQuerySuccess(intentName);
      this.output = createSuccessfulQueryOutput(intentName);
    } else {
      this.output = createFailedQueryOutput(intentName);
    }
  }

  /**
   * For "friendlikes" requests, checks if friend specified by the user is a valid friend, who is
   * listed in their list of friends. If not, it handles invalid friend requests and returns false.
   * If the friend is valid, it returns true.
   *
   * @return Boolean indicating whether the friend is valid
   */
  private Boolean isValidFriend() throws IOException {
    this.friendName = query.getFriendName();
    if (friendName == null) {
      this.output = "Which friend?";
      return false;
    }
    if (!PeopleUtils.hasFriend(userID, friendName)) {
      this.output = "I'm sorry. I don't recognize a " + friendName + " in your contact list.";
      return false;
    }
    return true;
  }

  /**
   * Returns fulfillment string for a successful retrieval of new Books from a new Query intent.
   * Includes: search, library, friends, mylikes, friendlikes intents.
   *
   * @param intentName name of detected intent
   * @return fulfillment string
   */
  private String createSuccessfulQueryOutput(String intentName) {
    if (intentName.equals("friends")) {
      return "Here are the books your friends like.";
    } else if (intentName.equals("mylikes")) {
      return "Here are your liked books.";
    } else if (intentName.equals("friendlikes")) {
      return "Here are " + query.getFriendName() + "'s liked books.";
    } else if (intentName.equals("library")) {
      return "Here are the books in your " + query.getBookshelfName() + " bookshelf.";
    }
    return "Here's what I found.";
  }

  /**
   * Returns fulfillment string for a failed retrieval of new Books from a new Query intent.
   * Includes: search, library, friends, mylikes, friendlikes intents.
   *
   * @param intentName name of detected intent
   * @return fulfillment string
   */
  private String createFailedQueryOutput(String intentName) {
    if (intentName.equals("friends")) {
      return "I'm sorry. Your friends haven't liked any books.";
    } else if (intentName.equals("mylikes")) {
      return "You haven't liked any books yet!";
    } else if (intentName.equals("friendlikes")) {
      return "I couldn't find any liked books for " + query.getFriendName() + ".";
    } else if (intentName.equals("library")) {
      return "There are no books in your " + query.getBookshelfName() + " bookshelf.";
    }
    return "I couldn't find any results. Can you try again?";
  }

  /**
   * Handles book more intents by: loading previous BookQuery, determining whether more books need
   * to be retrieved from Google Books API based on the next start index, and setting display to
   * next page of results if appropriate.
   */
  public void handleMoreIntent() throws IOException {
    // Loads query, prevStartIndex totalResults, resultsStored and increment startIndex
    loadBookQueryInfo(sessionID, queryID);
    loadIndicesInfo(sessionID, queryID);
    String ending = getFulfillmentEnding(query);
    this.startIndex = getNextStartIndex(prevStartIndex, totalResults);

    if (startIndex == -1) {
      this.output = "This is the last page of " + ending;
      this.startIndex = prevStartIndex;
    } else if (isNextPageOfBooksStored()) {
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
  public void handlePreviousIntent() throws IOException {
    // Loads query, prevStartIndex totalResults, resultsStored and increment startIndex
    loadBookQueryInfo(sessionID, queryID);
    loadIndicesInfo(sessionID, queryID);
    String ending = getFulfillmentEnding(query);
    this.startIndex = prevStartIndex - displayNum;

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
  public void handleBookInfoIntents(Map<String, Value> parameters) throws IOException {
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
  public void handleResultsIntent() throws IOException {
    // Loads query, prevStartIndex totalResults, resultsStored and increment startIndex
    loadBookQueryInfo(sessionID, queryID);
    loadIndicesInfo(sessionID, queryID);
    String ending = getFulfillmentEnding(query);
    if (query.isMyLibrary()) {
      this.userID = userService.getCurrentUser().getUserId();
      refreshBookshelf();
      if (resultsReturned == 0) {
        this.output = "There are no more books in " + ending;
      } else {
        setBookListDisplay();
        this.output = "Here's " + ending;
      }
    } else {
      this.startIndex = prevStartIndex;
      setBookListDisplay();
      String fulfillment = "Here are ";
      if (!query.getIntent().equals("friendlikes") && !query.getIntent().equals("mylikes")) {
        fulfillment += "the ";
      }
      this.output = fulfillment + ending;
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
    handleNewQueryIntents("library", parameters);
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
        || !lowerShelvesNames.contains(
            parameters.get("bookshelf").getStringValue().toLowerCase())) {
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
    loadIndicesInfo(sessionID, queryID);
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
  private void handleNewQuerySuccess(String intent) throws IOException {
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
   * Loads previous BookQuery information from the BookQuery matching the request's sessionID and
   * queryID and sets query parameter based on stored information
   *
   * @param sessionID ID of current user / session
   * @param queryID ID of query requested
   */
  private void loadBookQueryInfo(String sessionID, String queryID) {
    this.query = BooksMemoryUtils.getStoredBookQuery(sessionID, queryID, datastore);
  }

  /**
   * Loads previous Indices information from the BookQuery matching the request's sessionID and
   * queryID and sets prevStartIndex, resultsStored, totalResults based on stored information
   *
   * @param sessionID ID of current user / session
   * @param queryID ID of query requested
   */
  private void loadIndicesInfo(String sessionID, String queryID) {
    this.prevStartIndex =
        BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID, datastore);
    this.resultsStored =
        BooksMemoryUtils.getStoredIndices("resultsStored", sessionID, queryID, datastore);
    this.totalResults =
        BooksMemoryUtils.getStoredIndices("totalResults", sessionID, queryID, datastore);
  }

  /** Sets display to an ArrayList<Book> to display on user interface */
  private void setBookListDisplay() throws IOException {
    ArrayList<Book> booksToDisplay =
        BooksMemoryUtils.getStoredBooksToDisplay(
            displayNum, startIndex, sessionID, queryID, datastore, userService);
    this.display = listToJson(booksToDisplay);
  }

  /**
   * Sets display to Book specified in parameters and assigns like status to display on interface
   */
  private void setSingleBookDisplay(Book book) throws IOException {
    if (userService.isUserLoggedIn()) {
      ArrayList<Book> likedBooks = BooksMemoryUtils.getLikedBooksFromId(sessionID, "id", datastore);
      ArrayList<Book> friendsLikes = BooksMemoryUtils.getFriendsLikes(sessionID, datastore);
      book = BooksMemoryUtils.assignLikeCount(book, sessionID, friendsLikes, datastore);
      book = BooksMemoryUtils.assignLikeStatus(book, sessionID, likedBooks, datastore);
    }
    this.display = bookToJson(book);
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

  public static ArrayList<String> allLowerCaseList(ArrayList<String> list) {
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
    if (query.getIntent().equals("library")) {
      ending = "your " + query.getBookshelfName() + " bookshelf.";
    } else if (query.getIntent().equals("friendlikes")) {
      ending = query.getFriendName() + "'s  likes.";
    } else if (query.getIntent().equals("mylikes")) {
      ending = "your liked books.";
    } else {
      ending = "results.";
    }
    return ending;
  }

  private boolean isNextPageOfBooksStored() {
    return (startIndex + displayNum <= resultsStored || startIndex + displayNum >= totalResults);
  }
}
