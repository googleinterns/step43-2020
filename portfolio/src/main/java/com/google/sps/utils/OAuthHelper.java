package com.google.sps.utils;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.appengine.datastore.AppEngineDataStoreFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.BooksScopes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.servlet.ServletException;

/** This class contains methods that help with OAuth tasks */
public class OAuthHelper {
  /**
   * Loads stored Credential for userID, or null if one does not exist
   *
   * @return Credential object for current user
   */
  public static Credential loadUserCredential(String userID) throws IOException {
    AuthorizationCodeFlow flow = createFlow();
    return flow.loadCredential(userID);
  }

  /**
   * Creates a redirect URL for OAuth Servlets
   *
   * @return url String
   */
  public static String createRedirectUri() throws ServletException, IOException {
    GenericUrl url =
        new GenericUrl("https://8080-fabf4299-6bc0-403a-9371-600927588310.us-west1.cloudshell.dev");
    url.setRawPath("/oauth2callback");
    return url.build();
  }

  /**
   * Creates an AuthorizationCodeFlow to handle OAuth access tokens
   *
   * @return AuthorizationCodeFlow
   */
  public static AuthorizationCodeFlow createFlow() throws IOException {
    /*return new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
        new NetHttpTransport(),
        new JacksonFactory(),
        new GenericUrl("https://oauth2.googleapis.com/token"),
        new BasicAuthentication(getClientID(), getClientSecret()),
        getClientID(),
        "https://accounts.google.com/o/oauth2/auth").setScopes(BooksScopes.all()).setCredentialDataStore(
            StoredCredential.getDefaultDataStore(AppEngineDataStoreFactory.getDefaultInstance())).build();
    */
    return new GoogleAuthorizationCodeFlow.Builder(
            new NetHttpTransport(),
            JacksonFactory.getDefaultInstance(),
            getClientID(),
            getClientSecret(),
            BooksScopes.all())
        .setAccessType("offline")
        .setCredentialDataStore(
            StoredCredential.getDefaultDataStore(AppEngineDataStoreFactory.getDefaultInstance()))
        .build();
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
