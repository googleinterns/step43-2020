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
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volume.VolumeInfo.IndustryIdentifiers;
import com.google.gson.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * A Book object contains specific properties about a Volume object that will be used to create
 * output display and text for the user
 *
 * <p>A Book object is only created by createBook() function, ensuring that any Book object is only
 * created with valid parameters and that all Book objects have valid title and description
 * properties.
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
   * String or ArrayList<String>
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
    setISBN(volume);
    setTextSnippet(volume);
  }

  private void setTitle(Volume volume) {
    this.title = volume.getVolumeInfo().getTitle();
  }

  private void setDescription(Volume volume) {
    this.description = volume.getVolumeInfo().getDescription();
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

  private void setISBN(Volume volume) {
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

  public String getISBN() {
    return this.isbn;
  }

  public String getTextSnippet() {
    return this.textSnippet;
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
}
