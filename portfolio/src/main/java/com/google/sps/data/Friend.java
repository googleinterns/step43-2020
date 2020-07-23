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
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A Friend object contains specific properties about a Person object that will be used to create
 * output display and text
 *
 * <p>A Friend object is only created by createFriend() function, ensuring that any Friend object is
 * only created with valid parameters and that all Friend objects have at least one email address.
 */
public class Friend implements Serializable {

  private String name;
  private ArrayList<String> emailAddresses;

  /**
   * Creates a Friend object from a valid Person object that will be used to access Book likes or
   * throws an exception if the Person object is invalid or missing an email address
   *
   * @param person Person Object
   * @return Friend object
   */
  public static Friend createFriend(Person person) throws IOException {
    if (person == null || !hasValidParameters(person)) {
      throw new IOException();
    } else {
      Friend friend = new Friend(person);
      return friend;
    }
  }

  /**
   * Private Friend constructor, can only be called by createFriend() if Person parameter is not
   * null and Person object has a valid email. The constructor will set neccessary fields from the
   * Person object.
   *
   * <p>If Person object is missing any other properties, the properties will be set to an empty
   * String.
   *
   * @param person Person Object
   */
  private Friend(Person person) {
    setName(person);
    setEmailAddresses(person);
  }

  /**
   * Public Friend constructor, used for testing purposes. The constructor will set the following
   * fields from the parameters. The rest of the properties will be set to an Empty String.
   *
   * @param name String title of book
   * @param emailAddresses list of email addresses
   */
  public Friend(String name, ArrayList<String> emailAddresses) {
    this.name = name;
    this.emailAddresses = emailAddresses;
  }

  public void setName(Person person) {
    List<Name> names = person.getNames();
    if (names != null && names.size() > 0) {
      this.name = person.getNames().get(0).getDisplayName();
    } else {
      this.name = "";
    }
  }

  public void setEmailAddresses(Person person) {
    List<EmailAddress> emails = person.getEmailAddresses();
    ArrayList<String> stringEmails = new ArrayList<String>();
    if (emails != null && emails.size() > 0) {
      for (EmailAddress email : emails) {
        if (!email.getValue().isEmpty()) {
          stringEmails.add(email.getValue());
        }
      }
    }
    this.emailAddresses = stringEmails;
  }

  public String getName() {
    return this.name;
  }

  public ArrayList<String> getEmails() {
    return this.emailAddresses;
  }

  /**
   * Checks if person object has a valid title
   *
   * @param person person Object
   * @return boolean
   */
  public static boolean hasValidParameters(Person person) {
    List<EmailAddress> emails = person.getEmailAddresses();
    return (emails != null && emails.size() > 0);
  }
}
