package com.google.sps.servlets;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeCallbackServlet;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.utils.OAuthHelper;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/oauth2callback")
public class OAuthCallbackServlet extends AbstractAuthorizationCodeCallbackServlet {
  UserService userService = UserServiceFactory.getUserService();
  DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private static Logger log = LoggerFactory.getLogger(OAuthCallbackServlet.class);

  @Override
  protected void onSuccess(HttpServletRequest req, HttpServletResponse resp, Credential credential)
      throws ServletException, IOException {
    log.info("Success callback servlet");
    log.info("Credential: " + credential.getAccessToken());
    resp.sendRedirect("/");
  }

  @Override
  protected void onError(
      HttpServletRequest req, HttpServletResponse resp, AuthorizationCodeResponseUrl errorResponse)
      throws ServletException, IOException {
    log.error("Callback servlet OAuth error occured.");
    resp.sendRedirect("/");
  }

  @Override
  protected String getRedirectUri(HttpServletRequest req) throws ServletException, IOException {
    return OAuthHelper.createRedirectUri();
  }

  @Override
  protected AuthorizationCodeFlow initializeFlow() throws IOException {
    String userID = userService.getCurrentUser().getUserId();
    return OAuthHelper.createFlow(userID);
  }

  @Override
  protected String getUserId(HttpServletRequest req) throws ServletException, IOException {
    return userService.getCurrentUser().getUserId();
  }
}
