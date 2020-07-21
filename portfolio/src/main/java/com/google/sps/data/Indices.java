/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sps.data;

// Imports the Google Cloud client library
import com.google.api.services.books.*;
import com.google.gson.*;

/**
 * An Indices object contains specific properties stored about a user search query for Book results
 * from the Google Books API. It contains the start index of the last request, number of results
 * stored in Datastore, number of total results, the current page of results based on start index
 * and display number, and total pages of results based on display number output display and text
 * for the user.
 *
 * <p>An Indices object is created to pass information about indices to front-end via BookServlet
 */
public class Indices {
  private int startIndex;
  private int resultsStored;
  private int totalResults;
  private int currentPage;
  private int totalPages;
  private int displayNum;
  private boolean hasPrev;
  private boolean hasMore;

  /** Indices constructor initializes properties */
  public Indices(int startIndex, int resultsStored, int totalResults, int displayNum) {
    this.startIndex = startIndex;
    this.resultsStored = resultsStored;
    this.totalResults = totalResults;
    this.displayNum = displayNum;
    this.currentPage = startIndex / displayNum + 1;
    this.totalPages = getTotalPages(totalResults, displayNum);
    setHasPrev();
    setHasMore();
  }

  private void setHasPrev() {
    this.hasPrev = (startIndex - displayNum >= 0);
  }

  private void setHasMore() {
    this.hasMore = (startIndex + displayNum < totalResults);
  }

  public int getStartIndex() {
    return this.startIndex;
  }

  public int getResultsStored() {
    return this.resultsStored;
  }

  public int getTotalResults() {
    return this.totalResults;
  }

  public int getCurrentPage() {
    return this.currentPage;
  }

  public int getTotalPages() {
    return this.totalPages;
  }

  public boolean getHasMore() {
    return this.hasMore;
  }

  public boolean getHasPrev() {
    return this.hasPrev;
  }

  private int getTotalPages(int totalResults, int displayNum) {
    if (totalResults / displayNum == 0) {
      return 1;
    }
    return totalResults / displayNum;
  }
}
