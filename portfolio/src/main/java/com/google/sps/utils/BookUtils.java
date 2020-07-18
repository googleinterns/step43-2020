package com.google.sps.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.books.*;
import com.google.api.services.books.Books;
import com.google.api.services.books.Books.Volumes.List;
import com.google.api.services.books.model.Bookshelves;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;
import com.google.gson.*;
import com.google.sps.data.Book;
import com.google.sps.data.BookQuery;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class BookUtils {
  /**
   * This function returns an ArrayList of Book objects containing information from the Google Books
   * API based on the user's request, and throws an exception otherwise
   *
   * @param query BookQuery object containing parameters for user requested query
   * @param startIndex the index of the first result to return from Google Books API
   * @return ArrayList<Book> containing results
   */
  public static ArrayList<Book> getRequestedBooks(BookQuery query, int startIndex)
      throws IOException {
    Volumes volumes = getVolumes(query, startIndex);
    ArrayList<Book> results = volumesToBookList(volumes);
    return results;
  }

  /**
   * This function returns the number of total volumes from the Google Books API that match the
   * BookQuery request based on the user's input, and throws an exception otherwise
   *
   * @param query BookQuery object containing parameters for user requested query
   * @param startIndex the index of the first result to return from Google Books API
   * @return int total volumes found
   */
  public static int getTotalVolumesFound(BookQuery query, int startIndex) throws IOException {
    Volumes volumes = getVolumes(query, startIndex);
    return volumes.getTotalItems().intValue();
  }

  /**
   * This function returns a Volumes object containing the volumes from the Google Books API that
   * match the parameters in the BookQuery object, and throws an exception otherwise
   *
   * @param query BookQuery object containing parameters for user requested query
   * @param startIndex the index of the first result to return from Google Books API
   * @return Volumes object of results
   */
  public static Volumes getVolumes(BookQuery query, int startIndex) throws IOException {
    String queryString = query.getQueryString();
    Books books = getBooksContext();
    List list = books.volumes().list(queryString);

    if (query.getType() != null) {
      if (query.getType().equals("ebooks") || query.getType().equals("free-ebooks")) {
        list.setFilter(query.getType());
      } else if (query.getType().equals("magazines") || query.getType().equals("books")) {
        list.setPrintType(query.getType());
      }
    }

    if (query.getOrder() != null && query.getOrder().equals("newest")) {
      list.setOrderBy(query.getOrder());
    }

    if (query.getLanguage() != null) {
      list.setLangRestrict(query.getLanguage());
    }
    list.setMaxResults(Long.valueOf(40));
    list.setStartIndex(Long.valueOf(startIndex));

    return list.execute();
  }

  /**
   * This function builds and returns a Books object that can access a list of volumes the Google
   * Books API and throws an exception otherwise
   *
   * @return Books object
   */
  private static Books getBooksContext() throws IOException {
    String apiKey =
        new String(
            Files.readAllBytes(
                Paths.get(BookUtils.class.getResource("/files/apikey.txt").getFile())));
    GsonFactory gsonFactory = new GsonFactory();
    UrlFetchTransport transport = new UrlFetchTransport();

    Books books =
        new Books.Builder(transport, gsonFactory, null)
            .setApplicationName("APPNAME")
            .setGoogleClientRequestInitializer(new BooksRequestInitializer(apiKey))
            .build();
    return books;
  }

  /**
   * This function returns an ArrayList of Book objects from a Volumes object
   *
   * <p>If no valid Book objects can be constructed, it returns an empty ArrayList
   *
   * @param volumes Volumes object from Google Books API
   * @return ArrayList<Book>
   */
  private static ArrayList<Book> volumesToBookList(Volumes volumes) {
    if (volumes != null && volumes.getItems() != null) {
      ArrayList<Volume> vols = new ArrayList<Volume>(volumes.getItems());
      ArrayList<Book> books = new ArrayList<Book>();
      for (Volume vol : vols) {
        try {
          Book book = Book.createBook(vol);
          books.add(book);
        } catch (IOException e) {
          System.out.println("Result with invalid title was not added to list.");
        }
      }
      return books;
    }
    return new ArrayList<>();
  }

  /**
   * This function returns a Bookshelves object containing the Bookshelves from the Google Books API
   * that match the authenticated user's bookshelves and throws an exception otherwise
   *
   * @param userID unique userID
   * @return Volumes object of results
   */
  public static void getBookshelves(String userID) throws IOException {
    OAuthHelper helper = new OAuthHelper();
    Credential credential = helper.loadUserCredential(userID);
    if (credential.getExpiresInSeconds() <= 0) {
      if (credential.refreshToken()) {
        continue;
      }
    }
    System.out.println(credential.getMethod());
    System.out.println(credential.getAccessToken());
    System.out.println(credential.getExpiresInSeconds());
    System.out.println(credential.getTokenServerEncodedUrl());
    System.out.println(credential.getRefreshToken());

    Books books = getBooksContext();
    Bookshelves bookshelves =
        books.mylibrary().bookshelves().list().setOauthToken(credential.getAccessToken()).execute();
    System.out.println(bookshelves);
  }
}
