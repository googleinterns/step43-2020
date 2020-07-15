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

  public BooksAgent(String intentName, String userInput, Map<String, Value> parameters)
      throws IOException, IllegalArgumentException {
    this.displayNum = 5;
    this.intentName = intentName;
    this.userInput = userInput;
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
        // Delete stored BookQuery, Book results, totalResults, resultsReturned
        BooksMemoryUtils.deleteAllStoredBookInformation();

        // Store BookQuery, Book results, totalResults, resultsReturned
        BooksMemoryUtils.storeBooks(results, startIndex);
        BooksMemoryUtils.storeBookQuery(query);
        BooksMemoryUtils.storeIndices(startIndex, totalResults, resultsReturned, displayNum);

        ArrayList<Book> booksToDisplay =
            BooksMemoryUtils.getStoredBooksToDisplay(displayNum, startIndex);
        this.display = bookListToString(booksToDisplay);
        this.output = "Here's what I found.";
      } else {
        this.output = "I couldn't find any results. Can you try again?";
      }

    } else if (intentName.equals("more")) {
      // Load BookQuery, totalResults, resultsStored
      BookQuery prevQuery = BooksMemoryUtils.getStoredBookQuery();
      int prevStartIndex = BooksMemoryUtils.getStoredIndices("startIndex");
      int resultsStored = BooksMemoryUtils.getStoredIndices("resultsStored");
      int totalResults = BooksMemoryUtils.getStoredIndices("totalResults");

      // Increment startIndex
      int startIndex = getNextStartIndex(prevStartIndex, totalResults);
      if (startIndex == -1) {
        this.output = "I'm sorry, there are no more results.";
        return;
      } else if (startIndex + displayNum <= resultsStored) {
        // Replace indices
        BooksMemoryUtils.deleteStoredEntities("Indices");
        BooksMemoryUtils.storeIndices(startIndex, totalResults, resultsStored, displayNum);
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
          BooksMemoryUtils.deleteStoredEntities("Indices");

          // Store Book results and indices
          BooksMemoryUtils.storeBooks(results, startIndex);
          BooksMemoryUtils.storeIndices(startIndex, totalResults, newResultsStored, displayNum);
        }
      }
      ArrayList<Book> booksToDisplay =
          BooksMemoryUtils.getStoredBooksToDisplay(displayNum, startIndex);
      this.display = bookListToString(booksToDisplay);
      this.output = "Here's the next page of results.";

    } else if (intentName.equals("previous")) {
      // Load BookQuery, totalResults, resultsStored
      BookQuery prevQuery = BooksMemoryUtils.getStoredBookQuery();
      int prevStartIndex = BooksMemoryUtils.getStoredIndices("startIndex");
      int resultsStored = BooksMemoryUtils.getStoredIndices("resultsStored");
      int totalResults = BooksMemoryUtils.getStoredIndices("totalResults");

      // Increment startIndex
      int startIndex = prevStartIndex - displayNum;
      if (startIndex < -1) {
        this.output = "This is the first page of results.";
        startIndex = 0;
      } else {
        // Replace indices
        BooksMemoryUtils.deleteStoredEntities("Indices");
        BooksMemoryUtils.storeIndices(startIndex, totalResults, resultsStored, displayNum);
        this.output = "Here's the previous page of results.";
      }
      ArrayList<Book> booksToDisplay =
          BooksMemoryUtils.getStoredBooksToDisplay(displayNum, startIndex);
      this.display = bookListToString(booksToDisplay);

    } else if (intentName.equals("description")) {
      // Get requested order number from parameters
      int orderNum = (int) parameters.get("number").getNumberValue();

      // Retrieve requested book
      int prevStartIndex = BooksMemoryUtils.getStoredIndices("startIndex");
      Book requestedBook = BooksMemoryUtils.getBookFromOrderNum(orderNum, prevStartIndex);

      // Set output and display information
      this.output = "Here's a description for " + requestedBook.getTitle() + ".";
      this.display = bookToString(requestedBook);
      // Don't change any stored information
    } else if (intentName.equals("preview")) {
      // Get requested order number from parameters
      int orderNum = (int) parameters.get("number").getNumberValue();

      // Retrieve requested book
      int prevStartIndex = BooksMemoryUtils.getStoredIndices("startIndex");
      Book requestedBook = BooksMemoryUtils.getBookFromOrderNum(orderNum, prevStartIndex);

      // Set output and display information
      this.output = "Here's a preview of " + requestedBook.getTitle() + ".";
      this.display = bookToString(requestedBook);
      // Don't change any stored information

    } else if (intentName.equals("results")) {
      // Load Book results, totalResults, resultsReturned
      BookQuery prevQuery = BooksMemoryUtils.getStoredBookQuery();
      int prevStartIndex = BooksMemoryUtils.getStoredIndices("startIndex");
      int resultsStored = BooksMemoryUtils.getStoredIndices("resultsStored");
      int totalResults = BooksMemoryUtils.getStoredIndices("totalResults");
      ArrayList<Book> booksToDisplay =
          BooksMemoryUtils.getStoredBooksToDisplay(displayNum, prevStartIndex);

      this.display = bookListToString(booksToDisplay);
      this.output = "Here are the results.";
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
}
