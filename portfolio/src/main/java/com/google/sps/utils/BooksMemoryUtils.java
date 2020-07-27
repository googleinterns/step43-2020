package com.google.sps.utils;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.sps.agents.BooksAgent;
import com.google.sps.data.Book;
import com.google.sps.data.BookQuery;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.SerializationUtils;

public class BooksMemoryUtils {
  /**
   * This function stores each Book object an ArrayList of Book objects in DataStore as a Book
   * Entity with the corresponding properties
   *
   * @param books ArrayList of Book objects to store
   * @param startIndex index to start order at
   * @param sessionID unique id of session to store
   * @param queryID unique id (within sessionID) of query to store
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public static void storeBooks(
      ArrayList<Book> books,
      int startIndex,
      String sessionID,
      String queryID,
      DatastoreService datastore) {
    for (int i = 0; i < books.size(); ++i) {
      long timestamp = System.currentTimeMillis();
      Entity bookEntity = new Entity("Book");
      Key key = bookEntity.getKey();

      Book currentBook = books.get(i);
      currentBook.setOrder(i + startIndex);

      byte[] bookData = SerializationUtils.serialize(currentBook);
      Blob bookBlob = new Blob(bookData);

      bookEntity.setProperty("id", sessionID);
      bookEntity.setProperty("queryID", queryID);
      bookEntity.setProperty("title", currentBook.getTitle());
      bookEntity.setProperty("book", bookBlob);
      bookEntity.setProperty("order", i + startIndex);
      bookEntity.setProperty("timestamp", timestamp);
      datastore.put(bookEntity);
    }
  }

  /**
   * This function stores a BookQuery Object in DataStore as a BookQuery Entity with the
   * corresponding properties
   *
   * @param query BookQuery object to store
   * @param sessionID unique id of session to store
   * @param queryID unique id (within sessionID) of query to store
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public static void storeBookQuery(
      BookQuery query, String sessionID, String queryID, DatastoreService datastore) {
    long timestamp = System.currentTimeMillis();
    Entity bookQueryEntity = new Entity("BookQuery");

    byte[] bookQueryData = SerializationUtils.serialize(query);
    Blob bookQueryBlob = new Blob(bookQueryData);

    bookQueryEntity.setProperty("id", sessionID);
    bookQueryEntity.setProperty("queryID", queryID);
    bookQueryEntity.setProperty("bookQuery", bookQueryBlob);
    bookQueryEntity.setProperty("timestamp", timestamp);
    datastore.put(bookQueryEntity);
  }

  /**
   * This function stores a list of bookshelf names in DataStore as Bookshelves Entity for a given
   * authenticated user
   *
   * @param bookshelvesNames list of bookshelf names to store
   * @param userID unique id of user
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public static void storeBookshelfNames(
      ArrayList<String> bookshelvesNames, String userID, DatastoreService datastore) {
    long timestamp = System.currentTimeMillis();
    Entity bookshelfEntity = new Entity("Bookshelves");
    String listJson = BooksAgent.listToJson(bookshelvesNames);

    bookshelfEntity.setProperty("id", userID);
    bookshelfEntity.setProperty("names", listJson);
    bookshelfEntity.setProperty("timestamp", timestamp);
    datastore.put(bookshelfEntity);
  }

  /**
   * This function stores a the parameter integers in DataStore as a Indices Entity with the
   * corresponding properties
   *
   * @param startIndex index to start retrieving Volume objects from
   * @param resultsStored number of results stored
   * @param totalResults total matches in Google Book API
   * @param displayNum number of results displayed request
   * @param sessionID unique id of session to store
   * @param queryID unique id (within sessionID) of query to store
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public static void storeIndices(
      int startIndex,
      int totalResults,
      int resultsStored,
      int displayNum,
      String sessionID,
      String queryID,
      DatastoreService datastore) {
    long timestamp = System.currentTimeMillis();
    Entity indicesEntity = new Entity("Indices");

    indicesEntity.setProperty("id", sessionID);
    indicesEntity.setProperty("queryID", queryID);
    indicesEntity.setProperty("startIndex", startIndex);
    indicesEntity.setProperty("resultsStored", resultsStored);
    indicesEntity.setProperty("totalResults", totalResults);
    indicesEntity.setProperty("displayNum", displayNum);
    indicesEntity.setProperty("timestamp", timestamp);
    datastore.put(indicesEntity);
  }

  /**
   * This function deletes all Entitys in Datastore of type BookQuery, Book, and Indices for the
   * specified session id
   *
   * @param sessionID unique id of session to delete from
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public static void deleteAllStoredBookInformation(String sessionID, DatastoreService datastore) {
    deleteStoredEntities("BookQuery", sessionID, datastore);
    deleteStoredEntities("Book", sessionID, datastore);
    deleteStoredEntities("Indices", sessionID, datastore);
    deleteStoredEntities("Bookshelves", sessionID, datastore);
  }

  /**
   * This function deletes all Entitys in Datastore of type specified by parameter
   *
   * @param entityName name of Entity to delete
   * @param sessionID unique id of session to delete entities from
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public static void deleteStoredEntities(
      String entityName, String sessionID, DatastoreService datastore) {
    Filter currentUserFilter = new FilterPredicate("id", FilterOperator.EQUAL, sessionID);

    Query query = new Query(entityName).setFilter(currentUserFilter);
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
      datastore.delete(entity.getKey());
    }
  }

  /**
   * This function returns a list of Book objects of length numToRetrieve from the stored Book
   * objects in Datastore, starting at startIndex
   *
   * @param numToRetrieve number of Books to retrieve
   * @param startIndex index to start retrieving results from
   * @param sessionID unique id of session to retrieve from
   * @param queryID unique id (within sessionID) of query to store
   * @param datastore DatastoreService instance used to access Book info from database
   * @return ArrayList<Book>
   */
  public static ArrayList<Book> getStoredBooksToDisplay(
      int numToRetrieve,
      int startIndex,
      String sessionID,
      String queryID,
      DatastoreService datastore) {
    Filter idFilter = createSessionQueryFilter(sessionID, queryID);
    Query query = new Query("Book").setFilter(idFilter).addSort("order", SortDirection.ASCENDING);
    PreparedQuery results = datastore.prepare(query);

    ArrayList<Book> books = new ArrayList<>();
    int added = 0;
    for (Entity entity : results.asIterable()) {
      if (getStoredBookIndex(entity) >= startIndex) {
        if (added < numToRetrieve) {
          books.add(getBookFromEntity(entity));
          ++added;
        } else {
          break;
        }
      }
    }
    return books;
  }

  /**
   * This function returns the Book object stored in the Book Entity parameter in Datastore
   *
   * @param bookEntity Entity in Datastore
   * @return Book object
   */
  public static Book getBookFromEntity(Entity bookEntity) {
    Blob bookBlob = (Blob) bookEntity.getProperty("book");
    Book book = SerializationUtils.deserialize(bookBlob.getBytes());
    return book;
  }

  /**
   * This function returns the index of the the Book Entity parameter in Datastore
   *
   * @param bookEntity Entity in Datastore
   * @return int index
   */
  public static int getStoredBookIndex(Entity bookEntity) {
    Long lngValue = (Long) bookEntity.getProperty("order");
    return lngValue.intValue();
  }

  /**
   * This function returns the BookQuery object stored in Datastore that stores the parameters for
   * previous BookQuery
   *
   * @param sessionID unique id of session to retrieve from
   * @param queryID unique id (within sessionID) of query to store
   * @param datastore DatastoreService instance used to access Book info from database
   * @return BookQuery object
   */
  public static BookQuery getStoredBookQuery(
      String sessionID, String queryID, DatastoreService datastore) {
    Filter idFilter = createSessionQueryFilter(sessionID, queryID);
    Query query = new Query("BookQuery").setFilter(idFilter);

    Entity entity = datastore.prepare(query).asSingleEntity();
    Blob bookQueryBlob = (Blob) entity.getProperty("bookQuery");
    return SerializationUtils.deserialize(bookQueryBlob.getBytes());
  }

  /**
   * This function returns the list of bookshelf names stored in Datastore for the specified user
   *
   * @param userID unique id of user
   * @param datastore DatastoreService instance used to access Book info from database
   * @return ArrayList<String> of bookshelf names
   */
  public static ArrayList<String> getStoredBookshelfNames(
      String userID, DatastoreService datastore) {
    Filter currentUserFilter = new FilterPredicate("id", FilterOperator.EQUAL, userID);
    Query query = new Query("Bookshelves").setFilter(currentUserFilter);

    Entity entity = datastore.prepare(query).asSingleEntity();
    String listJson = (String) entity.getProperty("names");
    Gson gson = new Gson();
    Type listType = new TypeToken<ArrayList<String>>() {}.getType();
    ArrayList<String> names = gson.fromJson(listJson, listType);

    return names;
  }

  /**
   * This function returns the previous index specified by indexName stored in Datastore Indices
   * Entity
   *
   * @param indexName name of Indices: startIndex, resultsStored, totalResults, or displayNum
   * @param sessionID unique id of session to retrieve from
   * @param queryID unique id (within sessionID) of query to store
   * @param datastore DatastoreService instance used to access Book info from database
   * @return int startIndex
   */
  public static int getStoredIndices(
      String indexName, String sessionID, String queryID, DatastoreService datastore) {
    Filter idFilter = createSessionQueryFilter(sessionID, queryID);
    Query query = new Query("Indices").setFilter(idFilter);

    Entity entity = datastore.prepare(query).asSingleEntity();
    Long lngValue = (Long) entity.getProperty(indexName);
    return lngValue.intValue();
  }

  /**
   * This function returns the stored Book object that matches the parameter orderNum from Datastore
   * and throws an exception if the requested Book doesn't exist
   *
   * @param orderNum order number of book to retrieve
   * @param startIndex index to start retrieving results from
   * @param sessionID unique id of session to retrieve from
   * @param queryID unique id (within sessionID) of query to store
   * @param datastore DatastoreService instance used to access Book info from database
   * @return Book object
   */
  public static Book getBookFromOrderNum(
      int orderNum, int startIndex, String sessionID, String queryID, DatastoreService datastore)
      throws IllegalArgumentException {
    Filter idFilter = createSessionQueryFilter(sessionID, queryID);
    Query query = new Query("Book").setFilter(idFilter).addSort("order", SortDirection.ASCENDING);
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
      if (getStoredBookIndex(entity) == orderNum) {
        return getBookFromEntity(entity);
      }
    }
    throw new IllegalArgumentException();
  }

  /**
   * This function deletes all Entitys in Datastore of type BookQuery, Book, and Indices for any
   * session id
   */
  public static void deleteAllStoredBookInformation() {
    deleteStoredEntities("BookQuery");
    deleteStoredEntities("Book");
    deleteStoredEntities("Indices");
  }

  /**
   * This function deletes all Entitys in Datastore of type specified by parameter
   *
   * @param entityName name of Entity to delete
   */
  public static void deleteStoredEntities(String entityName) {
    Query query = new Query(entityName);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
      datastore.delete(entity.getKey());
    }
  }

  /**
   * This function returns the number of BookQuery Entities stored in Datastore with id sessionID
   *
   * @param sessionID session ID to retrieve stored Entities
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public static int getNumQueryStored(String sessionID, DatastoreService datastore) {
    Filter currentUserFilter = new FilterPredicate("id", FilterOperator.EQUAL, sessionID);
    Query query = new Query("BookQuery").setFilter(currentUserFilter);
    PreparedQuery pq = datastore.prepare(query);
    return Iterables.size(pq.asIterable());
  }

  /**
   * This function returns a boolean value indicating whether the authenticated user has Bookshelves
   * Entities stored in Datastore
   *
   * @param userID user ID to retrieve stored Entities
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public static boolean hasBookshelvesStored(String userID, DatastoreService datastore) {
    Filter currentUserFilter = new FilterPredicate("id", FilterOperator.EQUAL, userID);
    Query query = new Query("Bookshelves").setFilter(currentUserFilter);
    PreparedQuery pq = datastore.prepare(query);
    return (Iterables.size(pq.asIterable()) > 0);
  }

  /**
   * This function deletes all Entitys in Datastore of type specified by parameter with id property
   * of sessionID and queryID property of queryID
   *
   * @param entityName name of Entity to delete
   * @param sessionID unique id of session to delete entities from
   * @param queryID unique id (within session) to delete entities from
   * @param datastore DatastoreService instance used to access Book info from database
   */
  public static void deleteStoredEntities(
      String entityName, String sessionID, String queryID, DatastoreService datastore) {
    Filter idFilter = createSessionQueryFilter(sessionID, queryID);
    Query query = new Query(entityName).setFilter(idFilter);
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
      datastore.delete(entity.getKey());
    }
  }

  /**
   * This function returns a composite filter for Queries that retrieves Entitys with property id
   * equal to sessionID and property queryID equal to queryID
   *
   * @param sessionID unique id of session to delete entities from
   * @param queryID unique id (within session) to delete entities from
   */
  public static Filter createSessionQueryFilter(String sessionID, String queryID) {
    return new CompositeFilter(
        CompositeFilterOperator.AND,
        Arrays.asList(
            new FilterPredicate("id", FilterOperator.EQUAL, sessionID),
            new FilterPredicate("queryID", FilterOperator.EQUAL, queryID)));
  }
}
