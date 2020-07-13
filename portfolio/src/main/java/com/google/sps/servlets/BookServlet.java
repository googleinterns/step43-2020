package com.google.sps.servlets;

import com.google.gson.Gson;
import com.google.sps.data.Indices;
import com.google.sps.utils.BooksMemoryUtils;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/book")
public class BookServlet extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");

    int startIndex = BooksMemoryUtils.getStoredIndices("startIndex");
    int resultsStored = BooksMemoryUtils.getStoredIndices("resultsStored");
    int totalResults = BooksMemoryUtils.getStoredIndices("totalResults");
    int displayNum = BooksMemoryUtils.getStoredIndices("displayNum");

    Indices indices = new Indices(startIndex, resultsStored, totalResults, displayNum);
    String json = new Gson().toJson(indices);
    response.getWriter().write(json);
  }
}
