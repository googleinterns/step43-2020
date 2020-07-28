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
import com.google.api.services.books.v1.model.Volume;
import com.google.api.services.books.v1.model.Volume.VolumeInfo.IndustryIdentifiers;
import com.google.gson.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * A Book object contains specific properties about a Volume object that will be used to create
 * output display and text for the user
 *
 * <p>A Book object is only created by createBook() function, ensuring that any Book object is only
 * created with valid parameters and that all Book objects havea valid title property.
 */
public class Book implements Serializable {

  private String title;
  private String authors;
  private String publishedDate;
  private String description;
  private String averageRating;
  private String infoLink;
  private String thumbnailLink;
  private String buyLink;
  private Boolean embeddable;
  private String isbn;
  private String textSnippet;
  private int order;
  private String volumeId;
  private Boolean ebook = false;
  private Boolean isLiked = false;
  private ArrayList<String> likedBy;
  private int likeCount = 0;

  /**
   * Creates a Book object from a valid Volume object that will be used to build virtual assistant
   * output to users, or throws an exception if the Volume object is invalid or missing title and
   * description information
   *
   * @param volume Volume Object
   * @return Book object
   */
  public static Book createBook(Volume volume) throws IOException {
    if (volume == null || !hasValidParameters(volume)) {
      throw new IOException();
    } else {
      Book book = new Book(volume);
      return book;
    }
  }

  /**
   * Private Book constructor, can only be called by createBook() if Volume parameter is not null
   * and Volume object has a valid title and description. The constructor will set neccessary fields
   * from the Volume object.
   *
   * <p>If Volume object is missing any other properties, the properties will be set to an empty
   * String.
   *
   * @param volume Volume Object
   */
  private Book(Volume volume) {
    setTitle(volume);
    setAuthors(volume);
    setPublishedDate(volume);
    setDescription(volume);
    setRating(volume);
    setInfoLink(volume);
    setThumbnailLink(volume);
    setBuyLink(volume);
    setEmbeddable(volume);
    setIsbn(volume);
    setTextSnippet(volume);
    setVolumeId(volume);
    setEbook(volume);
    this.likedBy = new ArrayList<String>();
  }

  /**
   * Public Book constructor, used for testing purposes. The constructor will set the following
   * fields from the parameters. The rest of the properties will be set to an Empty String.
   *
   * @param title String title of book
   */
  public Book(String title) {
    this.title = title;
    this.authors = "";
    this.description = "";
    this.embeddable = false;
    this.isbn = "";
    this.publishedDate = "";
    this.averageRating = "";
    this.infoLink = "";
    this.thumbnailLink = "";
    this.buyLink = "";
    this.textSnippet = "";
    this.volumeId = "";
    this.likedBy = new ArrayList<String>();
  }

  /**
   * Public Book constructor, used for testing purposes with authors, description, embeddable, and
   * order fields specified. The rest of the properties will be set to an Empty String.
   *
   * @param title String title of book
   * @param authors String authors of book
   * @param description String description of book
   * @param embeddable Bool indicating whether book is embeddable
   * @param isbn String unique ISBN number
   * @param order order stored in datastore
   */
  public Book(
      String title,
      String authors,
      String description,
      Boolean embeddable,
      String isbn,
      int order) {
    this(title);
    this.authors = authors;
    this.description = description;
    this.embeddable = embeddable;
    this.isbn = isbn;
    this.order = order;
  }

  /**
   * Public Book constructor, used for autenticated user testing purposes with volumeId and order
   * properties specfied. The rest of the properties will be set to an Empty String.
   *
   * @param title String title of book
   * @param volumeId unique volume ID for book within Google Books API
   * @param order order stored in datastore
   */
  public Book(String title, String volumeId, int order) {
    this(title);
    this.volumeId = volumeId;
    this.order = order;
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof Book)) {
      return false;
    }
    Book otherBook = (Book) object;
    return otherBook.getVolumeId().equals(this.volumeId);
  }

  @Override
  public int hashCode() {
    return this.volumeId.hashCode();
  }

  public void setOrder(int order) {
    this.order = order;
  }

  private void setVolumeId(Volume volume) {
    this.volumeId = volume.getId();
  }

  private void setTitle(Volume volume) {
    this.title = volume.getVolumeInfo().getTitle();
  }

  private void setDescription(Volume volume) {
    this.description = volume.getVolumeInfo().getDescription();
  }

  private void setEbook(Volume volume) {
    if (hasValidSaleInfo(volume)) {
      this.ebook = volume.getSaleInfo().getIsEbook();
    }
  }

  private void setAuthors(Volume volume) {
    if (volume.getVolumeInfo().getAuthors() != null) {
      ArrayList<String> authors = new ArrayList<String>(volume.getVolumeInfo().getAuthors());
      this.authors = String.join(", ", authors);
    } else {
      this.authors = "";
    }
  }

  private void setPublishedDate(Volume volume) {
    if (volume.getVolumeInfo().getPublishedDate() != null) {
      String fullDate = volume.getVolumeInfo().getPublishedDate();
      this.publishedDate = fullDate.split("-")[0];
    } else {
      this.publishedDate = "";
    }
  }

  private void setRating(Volume volume) {
    if (volume.getVolumeInfo().getAverageRating() != null) {
      this.averageRating = String.valueOf(volume.getVolumeInfo().getAverageRating());
    } else {
      this.averageRating = "";
    }
  }

  private void setInfoLink(Volume volume) {
    if (volume.getVolumeInfo().getInfoLink() != null) {
      this.infoLink = volume.getVolumeInfo().getInfoLink();
    } else {
      this.infoLink = "";
    }
  }

  private void setThumbnailLink(Volume volume) {
    if (volume.getVolumeInfo().getImageLinks() != null) {
      if (volume.getVolumeInfo().getImageLinks().getThumbnail() != null) {
        this.thumbnailLink = volume.getVolumeInfo().getImageLinks().getThumbnail();
        return;
      }
    }
    this.thumbnailLink = "";
  }

  private void setBuyLink(Volume volume) {
    if (hasValidSaleInfo(volume)) {
      if (volume.getSaleInfo().getBuyLink() != null) {
        this.buyLink = volume.getSaleInfo().getBuyLink();
        return;
      }
    }
    this.buyLink = "";
  }

  private void setEmbeddable(Volume volume) {
    if (hasValidAccessInfo(volume)) {
      if (volume.getAccessInfo().getEmbeddable() != null) {
        this.embeddable = volume.getAccessInfo().getEmbeddable();
        return;
      }
    }
    this.embeddable = false;
  }

  private void setIsbn(Volume volume) {
    if (volume.getVolumeInfo().getIndustryIdentifiers() != null) {
      ArrayList<IndustryIdentifiers> industryIdentifiers =
          new ArrayList<IndustryIdentifiers>(volume.getVolumeInfo().getIndustryIdentifiers());
      for (IndustryIdentifiers id : industryIdentifiers) {
        if (id.getType() != null && id.getType().contains("ISBN")) {
          if (id.getIdentifier() != null) {
            this.isbn = id.getIdentifier();
            return;
          }
        }
      }
    }
    this.isbn = "";
  }

  private void setTextSnippet(Volume volume) {
    if (hasValidSearchInfo(volume)) {
      this.textSnippet = volume.getSearchInfo().getTextSnippet();
      return;
    }
    this.textSnippet = "";
  }

  public int getOrder() {
    return this.order;
  }

  public String getVolumeId() {
    return this.volumeId;
  }

  public String getTitle() {
    return this.title;
  }

  public String getAuthors() {
    return this.authors;
  }

  public String getPublishedDate() {
    return this.publishedDate;
  }

  public String getDescription() {
    return this.description;
  }

  public String getRating() {
    return this.averageRating;
  }

  public String getInfoLink() {
    return this.infoLink;
  }

  public String getThumbnailLink() {
    return this.thumbnailLink;
  }

  public String getBuyLink() {
    return this.buyLink;
  }

  public Boolean getEmbeddable() {
    return this.embeddable;
  }

  public String getIsbn() {
    return this.isbn;
  }

  public String getTextSnippet() {
    return this.textSnippet;
  }

  public ArrayList<String> getLikedBy() {
    return this.likedBy;
  }

  public int getLikeCount() {
    return this.likeCount;
  }

  public Boolean isEbook() {
    return this.ebook;
  }

  public void addToLikedBy(String name) {
    if (!this.likedBy.contains(name)) {
      this.likedBy.add(name);
      this.likeCount += 1;
    }
  }

  /**
   * Checks if Volume object has a valid title
   *
   * @param volume Volume Object
   * @return boolean
   */
  public static boolean hasValidParameters(Volume volume) {
    if (volume.getVolumeInfo() != null && volume.getVolumeInfo().getTitle() != null) {
      return true;
    }
    return false;
  }

  /**
   * Checks if Volume object has a valid accessInfo property
   *
   * @param volume Volume Object
   * @return boolean
   */
  public static boolean hasValidAccessInfo(Volume volume) {
    return volume.getAccessInfo() != null;
  }

  /**
   * Checks if Volume object has a valid saleInfo property
   *
   * @param volume Volume Object
   * @return boolean
   */
  public static boolean hasValidSaleInfo(Volume volume) {
    return volume.getSaleInfo() != null;
  }

  /**
   * Checks if Volume object has a valid searchInfo property
   *
   * @param volume Volume Object
   * @return boolean
   */
  public static boolean hasValidSearchInfo(Volume volume) {
    return volume.getSearchInfo() != null;
  }

  public void setIsLiked(Boolean bool) {
    this.isLiked = bool;
  }

  public void setLikedBy(ArrayList<String> likedByFriends) {
    this.likedBy = likedByFriends;
    this.likeCount = likedByFriends.size();
  }
}
