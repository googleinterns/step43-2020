package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.utils.BooksMemoryUtils;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Stores Book like information for user, or deletes stored Book liked information */
@WebServlet("/book-likes")
public class BookLikesServlet extends HttpServlet {

  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private UserService userService = UserServiceFactory.getUserService();

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String type = request.getParameter("type");
    int orderNum = Integer.parseInt(request.getParameter("orderNum"));
    String queryID = request.getParameter("query-id");

    if (type.equals("like")) {
      BooksMemoryUtils.likeBook(orderNum, queryID, userService, datastore);
    } else if (type.equals("unlike")) {
      BooksMemoryUtils.unlikeBook(orderNum, queryID, userService, datastore);
    }
  }
}
