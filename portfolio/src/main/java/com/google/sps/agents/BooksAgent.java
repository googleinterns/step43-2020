package com.google.sps.agents;

// Imports the Google Cloud client library
import com.google.gson.Gson;
import com.google.protobuf.Value;
import com.google.sps.data.Book;
import com.google.sps.data.BookQuery;
import com.google.sps.utils.BookUtils;
import com.google.sps.utils.BooksMemoryUtils;
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
  private BookQuery query;
  private int displayNum;
  private String sessionID;
  private String queryID;

  /**
   * BooksAgent constructor without queryID sets queryID property to the most recent queryID for the
   * specified sessionID
   */
  public BooksAgent(
      String intentName, String userInput, Map<String, Value> parameters, String sessionID)
      throws IOException, IllegalArgumentException {
    this(intentName, userInput, parameters, sessionID, null);
  }

  /** BooksAgent constructor with queryID */
  public BooksAgent(
      String intentName,
      String userInput,
      Map<String, Value> parameters,
      String sessionID,
      String queryID)
      throws IOException, IllegalArgumentException {
    this.displayNum = 5;
    this.intentName = intentName;
    this.userInput = userInput;
    this.sessionID = sessionID;
    this.queryID = queryID;
    if (queryID == null) {
      this.queryID = getMostRecentQueryID(sessionID);
    }
    setParameters(parameters);
  }

  @Override
  public void setParameters(Map<String, Value> parameters)
      throws IOException, IllegalArgumentException {
    if (intentName.equals("search")) {
      // Create new BookQuery request, sets startIndex at 0
      BookQuery query = BookQuery.createBookQuery(this.userInput, parameters);

      // Retrieve books
      int startIndex = 0;
      ArrayList<Book> results = BookUtils.getRequestedBooks(query, startIndex);
      int totalResults = BookUtils.getTotalVolumesFound(query, startIndex);
      int resultsReturned = results.size();

      if (resultsReturned > 0) {
        // Set new queryID
        this.queryID = getNextQueryID(sessionID);

        // Store BookQuery, Book results, totalResults, resultsReturned
        BooksMemoryUtils.storeBooks(results, startIndex, sessionID, queryID);
        BooksMemoryUtils.storeBookQuery(query, sessionID, queryID);
        BooksMemoryUtils.storeIndices(
            startIndex, totalResults, resultsReturned, displayNum, sessionID, queryID);

        ArrayList<Book> booksToDisplay =
            BooksMemoryUtils.getStoredBooksToDisplay(displayNum, startIndex, sessionID, queryID);
        this.display = bookListToString(booksToDisplay);
        this.redirect = queryID;
        this.output = "Here's what I found.";
      } else {
        this.output = "I couldn't find any results. Can you try again?";
      }

    } else if (intentName.equals("more")) {
      // Load BookQuery, totalResults, resultsStored
      BookQuery prevQuery = BooksMemoryUtils.getStoredBookQuery(sessionID, queryID);
      int prevStartIndex = BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID);
      int resultsStored = BooksMemoryUtils.getStoredIndices("resultsStored", sessionID, queryID);
      int totalResults = BooksMemoryUtils.getStoredIndices("totalResults", sessionID, queryID);

      // Increment startIndex
      int startIndex = getNextStartIndex(prevStartIndex, totalResults);
      if (startIndex == -1) {
        this.output = "I'm sorry, there are no more results.";
        return;
      } else if (startIndex + displayNum <= resultsStored) {
        // Replace indices
        BooksMemoryUtils.deleteStoredEntities("Indices", sessionID, queryID);
        BooksMemoryUtils.storeIndices(
            startIndex, totalResults, resultsStored, displayNum, sessionID, queryID);
      } else {
        // Retrieve books
        ArrayList<Book> results = BookUtils.getRequestedBooks(prevQuery, startIndex);
        int resultsReturned = results.size();
        int newResultsStored = resultsReturned + resultsStored;

        // Even though there are more results, if Volume objects don't have a title
        // then we don't create any Book objects, so we still have to check for an empty Book list
        if (resultsReturned == 0) {
          this.output = "I'm sorry, there are no more results.";
          return;
        } else {
          // Delete stored Book results and indices
          BooksMemoryUtils.deleteStoredEntities("Indices", sessionID, queryID);

          // Store Book results and indices
          BooksMemoryUtils.storeBooks(results, startIndex, sessionID, queryID);
          BooksMemoryUtils.storeIndices(
              startIndex, totalResults, newResultsStored, displayNum, sessionID, queryID);
        }
      }
      ArrayList<Book> booksToDisplay =
          BooksMemoryUtils.getStoredBooksToDisplay(displayNum, startIndex, sessionID, queryID);
      this.display = bookListToString(booksToDisplay);
      this.redirect = queryID;
      this.output = "Here's the next page of results.";

    } else if (intentName.equals("previous")) {
      // Load BookQuery, totalResults, resultsStored
      BookQuery prevQuery = BooksMemoryUtils.getStoredBookQuery(sessionID, queryID);
      int prevStartIndex = BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID);
      int resultsStored = BooksMemoryUtils.getStoredIndices("resultsStored", sessionID, queryID);
      int totalResults = BooksMemoryUtils.getStoredIndices("totalResults", sessionID, queryID);

      // Increment startIndex
      int startIndex = prevStartIndex - displayNum;
      if (startIndex < -1) {
        this.output = "This is the first page of results.";
        startIndex = 0;
      } else {
        // Replace indices
        BooksMemoryUtils.deleteStoredEntities("Indices", sessionID, queryID);
        BooksMemoryUtils.storeIndices(
            startIndex, totalResults, resultsStored, displayNum, sessionID, queryID);
        this.output = "Here's the previous page of results.";
      }
      ArrayList<Book> booksToDisplay =
          BooksMemoryUtils.getStoredBooksToDisplay(displayNum, startIndex, sessionID, queryID);
      this.display = bookListToString(booksToDisplay);
      this.redirect = queryID;

    } else if (intentName.equals("description")) {
      // Get requested order number from parameters
      int orderNum = (int) parameters.get("number").getNumberValue();

      // Retrieve requested book
      int prevStartIndex = BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID);
      Book requestedBook =
          BooksMemoryUtils.getBookFromOrderNum(orderNum, prevStartIndex, sessionID, queryID);

      // Set output and display information
      this.output = "Here's a description for " + requestedBook.getTitle() + ".";
      this.display = bookToString(requestedBook);
      this.redirect = queryID;
      // Don't change any stored information
    } else if (intentName.equals("preview")) {
      // Get requested order number from parameters
      int orderNum = (int) parameters.get("number").getNumberValue();

      // Retrieve requested book
      int prevStartIndex = BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID);
      Book requestedBook =
          BooksMemoryUtils.getBookFromOrderNum(orderNum, prevStartIndex, sessionID, queryID);

      // Set output and display information
      this.output = "Here's a preview of " + requestedBook.getTitle() + ".";
      this.display = bookToString(requestedBook);
      this.redirect = queryID;
      // Don't change any stored information

    } else if (intentName.equals("results")) {
      // Load Book results, totalResults, resultsReturned
      BookQuery prevQuery = BooksMemoryUtils.getStoredBookQuery(sessionID, queryID);
      int prevStartIndex = BooksMemoryUtils.getStoredIndices("startIndex", sessionID, queryID);
      int resultsStored = BooksMemoryUtils.getStoredIndices("resultsStored", sessionID, queryID);
      int totalResults = BooksMemoryUtils.getStoredIndices("totalResults", sessionID, queryID);
      ArrayList<Book> booksToDisplay =
          BooksMemoryUtils.getStoredBooksToDisplay(displayNum, prevStartIndex, sessionID, queryID);

      this.display = bookListToString(booksToDisplay);
      this.output = "Here are the results.";
      this.redirect = queryID;
      // Don't change any stored information
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

  private int getNextStartIndex(int prevIndex, int total) {
    int nextIndex = prevIndex + displayNum;
    if (nextIndex < total) {
      return nextIndex;
    }
    return -1;
  }

  private String bookToString(Book book) {
    Gson gson = new Gson();
    return gson.toJson(book);
  }

  private String bookListToString(ArrayList<Book> books) {
    Gson gson = new Gson();
    return gson.toJson(books);
  }

  private String getMostRecentQueryID(String sessionID) {
    int queryNum = BooksMemoryUtils.getNumQueryStored(sessionID);
    String queryID = "query-" + Integer.toString(queryNum);
    return queryID;
  }

  private String getNextQueryID(String sessionID) {
    int queryNum = BooksMemoryUtils.getNumQueryStored(sessionID) + 1;
    String queryID = "query-" + Integer.toString(queryNum);
    return queryID;
  }
}
