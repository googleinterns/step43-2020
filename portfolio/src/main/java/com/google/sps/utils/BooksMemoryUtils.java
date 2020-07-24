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
import com.google.appengine.api.users.UserService;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.sps.agents.BooksAgent;
import com.google.sps.data.Book;
import com.google.sps.data.BookComparator;
import com.google.sps.data.BookQuery;
import com.google.sps.data.Friend;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
   * objects in Datastore, starting at startIndex. It assigns the appropriate like status for the
   * book based on the unique sessionID (user) and the appropriate like count, based on the user's
   * friends.
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
      DatastoreService datastore)
      throws IOException {
    Filter idFilter = createSessionQueryFilter(sessionID, queryID);
    Query query = new Query("Book").setFilter(idFilter).addSort("order", SortDirection.ASCENDING);
    PreparedQuery results = datastore.prepare(query);
    ArrayList<String> likedBooks = getLikedBookIds(sessionID, datastore);
    ArrayList<Book> friendsLikes = getFriendsLikes(sessionID, datastore);

    ArrayList<Book> books = new ArrayList<>();
    int added = 0;
    for (Entity entity : results.asIterable()) {
      if (getStoredBookIndex(entity) >= startIndex) {
        if (added < numToRetrieve) {
          Book book = getBookFromEntity(entity);
          Book bookWithLikeCount = assignLikeCount(book, sessionID, friendsLikes, datastore);
          Book bookToDisplay =
              assignLikeStatus(bookWithLikeCount, sessionID, likedBooks, datastore);
          books.add(bookToDisplay);
          ++added;
        } else {
          break;
        }
      }
    }
    return books;
  }

  /**
   * This function assigns the appropriate like status for a Book object based on the information in
   * Datastore for the userID likes.
   *
   * @param book Book object to assign like status for, based on information stored in Datastore
   * @param userID unique id of user
   * @param likedBooks list of Book objects that the user likes
   * @param datastore DatastoreService instance used to access Book info from database
   * @return Book
   */
  public static Book assignLikeStatus(
      Book book, String userID, ArrayList<String> likedBooks, DatastoreService datastore) {
    book.setIsLiked(likedBooks.contains(book.getVolumeId()));
    return book;
  }

  /**
   * This function assigns the appropriate like count for a Book object based on the information in
   * Datastore for the userID's friends' likes.
   *
   * @param book Book object to assign like status for, based on information stored in Datastore
   * @param userID unique id of user
   * @param friendsLikes list of Book objects that the user's friends like, including likedBy
   *     information
   * @param datastore DatastoreService instance used to access Book info from database
   * @return Book
   */
  public static Book assignLikeCount(
      Book book, String userID, ArrayList<Book> friendsLikes, DatastoreService datastore) {
    if (friendsLikes.contains(book)) {
      ArrayList<String> likedByList = friendsLikes.get(friendsLikes.indexOf(book)).getLikedBy();
      book.setLikedBy(likedByList);
    }
    return book;
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

  /**
   * This function stores a Book object and userEmail in a LikedBook entity in Datastore
   *
   * @param orderNum index of book to like
   * @param startIndex index to start order at
   * @param sessionID unique id of session to store
   * @param queryID unique id (within sessionID) of query to store
   * @param datastore DatastoreService instance used to access Book info from database
   * @param userService UserService instance to access userID and other user info.
   */
  public static void likeBook(
      int orderNum, String queryID, UserService userService, DatastoreService datastore) {
    String userID = userService.getCurrentUser().getUserId();
    String userEmail = userService.getCurrentUser().getEmail();
    int startIndex = getStoredIndices("startIndex", userID, queryID, datastore);
    Book bookToLike = getBookFromOrderNum(orderNum, startIndex, userID, queryID, datastore);
    byte[] bookData = SerializationUtils.serialize(bookToLike);
    Blob bookBlob = new Blob(bookData);

    Entity likedBookEntity = new Entity("LikedBook");
    likedBookEntity.setProperty("id", userID);
    likedBookEntity.setProperty("userEmail", userEmail);
    likedBookEntity.setProperty("volumeId", bookToLike.getVolumeId());
    likedBookEntity.setProperty("book", bookBlob);
    datastore.put(likedBookEntity);
  }

  /**
   * This function deletes a stored LikedBook Entity for the book and user specified in Datastore
   *
   * @param orderNum index of book to like
   * @param startIndex index to start order at
   * @param sessionID unique id of session to store
   * @param queryID unique id (within sessionID) of query to store
   * @param datastore DatastoreService instance used to access Book info from database
   * @param userService UserService instance to access userID and other user info.
   */
  public static void unlikeBook(
      int orderNum, String queryID, UserService userService, DatastoreService datastore) {
    String userID = userService.getCurrentUser().getUserId();
    int startIndex = getStoredIndices("startIndex", userID, queryID, datastore);
    Book bookToUnlike = getBookFromOrderNum(orderNum, startIndex, userID, queryID, datastore);
    String volumeID = bookToUnlike.getVolumeId();

    Filter filter =
        new CompositeFilter(
            CompositeFilterOperator.AND,
            Arrays.asList(
                new FilterPredicate("id", FilterOperator.EQUAL, userID),
                new FilterPredicate("volumeId", FilterOperator.EQUAL, volumeID)));

    Query query = new Query("LikedBook").setFilter(filter);
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
      datastore.delete(entity.getKey());
    }
  }

  /**
   * This function returns a list of Book objects from the stored LikedBook Entities in Datastore
   * for all friends of the given userID
   *
   * @param userID unique id of user
   * @param datastore DatastoreService instance used to access Book info from database
   * @return ArrayList<Book> books liked by friends
   */
  public static ArrayList<Book> getFriendsLikes(String userID, DatastoreService datastore)
      throws IOException {
    ArrayList<Book> friendsLikes = new ArrayList<Book>();
    for (Friend friend : PeopleUtils.getFriends(userID)) {
      for (String email : friend.getEmails()) {
        String name = friend.getName();
        if (!friend.hasName()) {
          name = email;
        }
        ArrayList<Book> booksLikedByEmail = getLikedBooksFromId(email, "userEmail", datastore);
        for (Book likedBook : booksLikedByEmail) {
          if (friendsLikes.contains(likedBook)) {
            Book bookInList = friendsLikes.get(friendsLikes.indexOf(likedBook));
            bookInList.addToLikedBy(name);
          } else {
            likedBook.addToLikedBy(name);
            friendsLikes.add(likedBook);
          }
        }
      }
    }
    Collections.sort(friendsLikes, new BookComparator());
    return friendsLikes;
  }

  /**
   * This function returns a list of Book objects from the stored LikedBook Entities in Datastore
   * for the specified friend of the userID
   *
   * @param userID unique id of user
   * @param friendName name of friend to retrive liked books of
   * @param datastore DatastoreService instance used to access Book info from database
   * @return ArrayList<Book> books liked by friends
   */
  public static ArrayList<Book> getLikesOfFriend(
      String userID, String friendName, DatastoreService datastore) throws IOException {
    ArrayList<Book> friendsLikes = getFriendsLikes(userID, datastore);
    ArrayList<Book> individualFriendLikes = new ArrayList<Book>();
    for (Book likedBook : friendsLikes) {
      ArrayList<String> likedByLowerCase = BooksAgent.allLowerCaseList(likedBook.getLikedBy());
      if (likedByLowerCase.contains(friendName.toLowerCase())) {
        individualFriendLikes.add(likedBook);
      }
    }
    Collections.sort(individualFriendLikes, new BookComparator());
    return individualFriendLikes;
  }

  /**
   * This function returns a list of Book objects from the stored LikedBook Entities in Datastore
   * for the given id
   *
   * @param id id of user (either email address or userID)
   * @param property LikedBook property to specify filter (either userEmail or id)
   * @param datastore DatastoreService instance used to access Book info from database
   * @return ArrayList<Book>
   */
  public static ArrayList<Book> getLikedBooksFromId(
      String id, String property, DatastoreService datastore) {
    Filter idFilter = new FilterPredicate(property, FilterOperator.EQUAL, id);
    Query query = new Query("LikedBook").setFilter(idFilter);
    PreparedQuery results = datastore.prepare(query);

    ArrayList<Book> likedBooks = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      likedBooks.add(getBookFromEntity(entity));
    }
    return likedBooks;
  }

  /**
   * This function returns a list of Book objects of length numToRetrieve from the stored Book
   * objects in Datastore, starting at startIndex
   *
   * @param userID unique id of user
   * @param datastore DatastoreService instance used to access Book info from database
   * @return ArrayList<Book>
   */
  public static ArrayList<String> getLikedBookIds(String userID, DatastoreService datastore) {
    Filter idFilter = new FilterPredicate("id", FilterOperator.EQUAL, userID);
    Query query = new Query("LikedBook").setFilter(idFilter);
    PreparedQuery results = datastore.prepare(query);

    ArrayList<String> likedBookIds = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      likedBookIds.add((String) entity.getProperty("volumeId"));
    }
    return likedBookIds;
  }
}
