package com.google.sps.data;

import java.util.Comparator;

/* This class compares Book objects based on like count */
public class BookComparator implements Comparator<Book> {
  public int compare(Book firstBook, Book secondBook) {
    return secondBook.getLikeCount() - firstBook.getLikeCount();
  }
}
