package com.google.sps.data;

import java.util.Comparator;

/* This class compares Book objects based on like count, with ties broken alphabetically*/
public class BookComparator implements Comparator<Book> {
  public int compare(Book firstBook, Book secondBook) {
    if (secondBook.getLikeCount() - firstBook.getLikeCount() == 0) {
      return (firstBook.getTitle()).compareTo(secondBook.getTitle());
    }
    return secondBook.getLikeCount() - firstBook.getLikeCount();
  }
}
