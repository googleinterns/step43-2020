package com.google.sps.utils;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.sps.data.Book;
import com.google.sps.data.BookQuery;
import java.util.ArrayList;
import org.apache.commons.lang3.SerializationUtils;

public class BooksMemoryUtils {
  /**
   * This function stores each Book object an ArrayList of Book objects in DataStore as a Book
   * Entity with the corresponding properties
   *
   * @param books ArrayList of Book objects to store
   * @param startIndex index to start order at
   */
  public static void storeBooks(ArrayList<Book> books, int startIndex) {
    for (int i = 0; i < books.size(); ++i) {
      long timestamp = System.currentTimeMillis();
      Book currentBook = books.get(i);
      Entity bookEntity = new Entity("Book");

      byte[] bookData = SerializationUtils.serialize(currentBook);
      Blob bookBlob = new Blob(bookData);

      bookEntity.setProperty("title", currentBook.getTitle());
      bookEntity.setProperty("book", bookBlob);
      bookEntity.setProperty("order", i + startIndex);
      bookEntity.setProperty("timestamp", timestamp);
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      datastore.put(bookEntity);
    }
  }

  /**
   * This function stores a BookQuery Object in DataStore as a BookQuery Entity with the
   * corresponding properties
   *
   * @param query BookQuery object to store
   */
  public static void storeBookQuery(BookQuery query) {
    long timestamp = System.currentTimeMillis();
    Entity bookQueryEntity = new Entity("BookQuery");

    byte[] bookQueryData = SerializationUtils.serialize(query);
    Blob bookQueryBlob = new Blob(bookQueryData);

    bookQueryEntity.setProperty("bookQuery", bookQueryBlob);
    bookQueryEntity.setProperty("timestamp", timestamp);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(bookQueryEntity);
  }

  /**
   * This function stores a the parameter integers in DataStore as a Indices Entity with the
   * corresponding properties
   *
   * @param startIndex index to start retrieving Volume objects from
   * @param resultsStored number of results stored
   * @param totalResults total matches in Google Book API
   * @param displayNum number of results displayed request
   */
  public static void storeIndices(
      int startIndex, int totalResults, int resultsStored, int displayNum) {
    long timestamp = System.currentTimeMillis();
    Entity indicesEntity = new Entity("Indices");
    indicesEntity.setProperty("startIndex", startIndex);
    indicesEntity.setProperty("resultsStored", resultsStored);
    indicesEntity.setProperty("totalResults", totalResults);
    indicesEntity.setProperty("displayNum", displayNum);
    indicesEntity.setProperty("timestamp", timestamp);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(indicesEntity);
  }

  /** This function deletes all Entitys in Datastore of type BookQuery, Book, and Indices */
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
    Query query = new Query(entityName).addSort("timestamp", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
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
   * @return ArrayList<Book>
   */
  public static ArrayList<Book> getStoredBooksToDisplay(int numToRetrieve, int startIndex) {
    Query query = new Query("Book").addSort("order", SortDirection.ASCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
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
   * This function returns the BookQuery object stored in Datastore, storing the parameters for
   * previous BookQuery
   *
   * @return BookQuery object
   */
  public static BookQuery getStoredBookQuery() {
    Query query = new Query("BookQuery");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity entity = datastore.prepare(query).asSingleEntity();
    Blob bookQueryBlob = (Blob) entity.getProperty("bookQuery");
    return SerializationUtils.deserialize(bookQueryBlob.getBytes());
  }

  /**
   * This function returns the previous index specified by indexName stored in Datastore Indices
   * Entity
   *
   * @param indexName name of Indices: startIndex, resultsStored, totalResults, or displayNum
   * @return int startIndex
   */
  public static int getStoredIndices(String indexName) {
    Query query = new Query("Indices");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity entity = datastore.prepare(query).asSingleEntity();
    Long lngValue = (Long) entity.getProperty(indexName);
    return lngValue.intValue();
  }
}
