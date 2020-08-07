/*
 * Copyright 2019 Google LLC
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

package com.google.sps.utils;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.appengine.datastore.AppEngineDataStoreFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.v1.BooksScopes;
import com.google.api.services.people.v1.PeopleServiceScopes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;

/** This class contains methods that help with OAuth 2.0. handling. */
public class OAuthHelper {
  static final String REDIRECT_URL_BASE =
      "https://8080-fabf4299-6bc0-403a-9371-600927588310.us-west1.cloudshell.dev";

  /**
   * Loads stored Credential for userID, or null if one does not exist.
   *
   * @param userID current unique user ID
   * @return Credential object for current user
   */
  public static Credential loadUserCredential(String userID) throws IOException {
    AuthorizationCodeFlow flow = createFlow(userID);
    return flow.loadCredential(userID);
  }

  /**
   * Loads and updates stored Credential for userID if needed, or returns null if one does not
   * exist.
   *
   * @param userID current unique user ID
   * @return Credential object for current user
   */
  public static Credential loadUpdatedCredential(String userID) throws IOException {
    Credential credential = loadUserCredential(userID);
    if (credential == null) {
      return credential;
    }
    if (credential.getExpiresInSeconds() <= 60) {
      credential.refreshToken();
      String refreshToken = credential.getRefreshToken();
      credential = credential.setRefreshToken(refreshToken);
    }
    return credential;
  }

  /**
   * Creates a redirect URL for OAuth Servlets.
   *
   * @return url String
   */
  public static String createRedirectUri() throws ServletException, IOException {
    GenericUrl url = new GenericUrl(REDIRECT_URL_BASE);
    url.setRawPath("/oauth2callback");
    return url.build();
  }

  /**
   * Creates an AuthorizationCodeFlow to handle OAuth access tokens.
   *
   * @param userID current unique user ID
   * @return AuthorizationCodeFlow
   */
  public static AuthorizationCodeFlow createFlow(String userID) throws IOException {
    Set<String> scopes = new HashSet<String>();
    scopes.add(BooksScopes.BOOKS);
    scopes.add(PeopleServiceScopes.USERINFO_PROFILE);
    scopes.add(PeopleServiceScopes.USERINFO_EMAIL);
    scopes.add(PeopleServiceScopes.CONTACTS_READONLY);
    return new GoogleAuthorizationCodeFlow.Builder(
            new NetHttpTransport(),
            JacksonFactory.getDefaultInstance(),
            getClientID(),
            getClientSecret(),
            scopes)
        .setAccessType("offline")
        .setApprovalPrompt("force")
        .setCredentialDataStore(
            StoredCredential.getDefaultDataStore(AppEngineDataStoreFactory.getDefaultInstance()))
        .addRefreshListener(
            new DataStoreCredentialRefreshListener(
                userID,
                StoredCredential.getDefaultDataStore(
                    AppEngineDataStoreFactory.getDefaultInstance())))
        .build();
  }

  /**
   * This function determines if the current user has stored book credentials.
   *
   * @param userID ID of current user logged in
   * @return boolean indicating if user has book credentials
   */
  public boolean hasBookAuthentication(String userID) throws IOException {
    return (loadUserCredential(userID) != null);
  }

  public static String getClientID() throws IOException {
    return new String(
        Files.readAllBytes(
            Paths.get(OAuthHelper.class.getResource("/files/clientid.txt").getFile())));
  }

  public static String getClientSecret() throws IOException {
    return new String(
        Files.readAllBytes(
            Paths.get(OAuthHelper.class.getResource("/files/clientsecret.txt").getFile())));
  }
}
