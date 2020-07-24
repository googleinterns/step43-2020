
/**
 * This function places a book div element in the container specified by the 
 * parameter and updates the scroll to the top of the table displayed
 *
 * @param bookDiv element containing book table
 * @param container container to place book display in
 * @param queryID appropriate queryID for results in this element
 */
function placeBookDisplay(bookDiv, container, queryID) {
  var container = document.getElementsByName(container)[0];
  var bookContainer = document.createElement('div');
  bookContainer.className = queryID;
  bookContainer.appendChild(bookDiv);
  bookContainer.insertAdjacentHTML('beforeend', '<br>');
  container.appendChild(bookContainer)
  updateBookScroll(queryID);
}

/**
 * This function creates a Book container containing a 
 * <table></table> element with information about Book objects from the 
 * json bookResults parameter
 *
 * @param bookResults json ArrayList<Book> objects
 * @param queryID appropriate queryID for results in this element
 * @return booksDiv element containing a book results table 
 */
function createBookContainer(bookResults, queryID) {
    var booksList = JSON.parse(bookResults);
    var booksDiv = document.createElement("div"); 
    booksDiv.className = "book-div";
    var bookTable = document.createElement("table"); 
    bookTable.className = "book-table";
    booksList.forEach((book) => {
      bookTable.appendChild(createBookRow(book, queryID));
    });
    bookTable.appendChild(createTableFooter(queryID));
    booksDiv.appendChild(bookTable);
    return booksDiv;
}

/**
 * This function creates a Book container containing a 
 * <table></table> element with information about Bookshelf names from the 
 * json bookResults parameter
 *
 * @param bookResults json ArrayList<Book> objects
 * @return booksDiv element containing a book results table 
 */
function createBookshelfContainer(bookResults) {
  var bookshelfList = JSON.parse(bookResults);
  var booksDiv = document.createElement("div"); 
  booksDiv.className = "book-div";
  var bookTable = document.createElement("table"); 
  bookTable.className = "book-table";
  bookshelfList.forEach((bookName) => {
    bookTable.appendChild(createBookShelfRow(bookName));
  });
  booksDiv.appendChild(bookTable);
  return booksDiv;
}

/**
 * This function creates a row <tr></tr> element containing information from the
 * parameter bookshelf name to be added to the book table
 *
 * @param bookshelfName name of bookshelf
 * @return bookRow element to be added to table
 */
function createBookShelfRow(bookshelfName) {
  const bookRow = document.createElement('tr');
  bookRow.className = "book-row";

  const bookshelfColumn = document.createElement('td');
  bookshelfColumn.className = "bookshelf-name";

  var bookshelfButton = document.createElement("button");
  bookshelfButton.className = "bookshelf-button";
  bookshelfButton.insertAdjacentHTML('afterbegin', bookshelfName);
  bookshelfButton.addEventListener("click", function () {
    goToBookshelf('books.library', bookshelfName);
  });
  bookshelfColumn.appendChild(bookshelfButton);
  bookRow.appendChild(bookshelfColumn);
  return bookRow;
}

/**
 * This function creates a table footer <tr></tr> element containing current 
 * page index and previous and next buttons (if applicable)
 *
 * @param queryID appropriate queryID for results in this element
 * @return footerRow element to be added to the bottom of table
 */
function createTableFooter(queryID) {
  const footerRow = document.createElement('tr');
  footerRow.className = "book-row";
  fetch('/book-indices'+ '?session-id=' + sessionId + '&query-id=' + queryID).then(response => response.json()).then((indices) => {
    const prevColumn = createColumn("previous", indices.hasPrev, queryID);
    const pageColumn = createPageColumn(indices);
    const moreColumn = createColumn("more", indices.hasMore, queryID);
    footerRow.appendChild(prevColumn);
    footerRow.appendChild(pageColumn);
    footerRow.appendChild(moreColumn);
  });
  return footerRow;
}

/**
 * This function creates a <td></td> element containing a button (previous or more)
 * specified by the parameter type if makeColumn is True
 *
 * @param type column type to make
 * @param makeColumn boolean specifying if column is appropriate to make
 * @param queryID appropriate queryID for results in this element
 * @return prevText of <td></td> element
 */
function createColumn(type, makeColumn, queryID) {
  const column = document.createElement('td');

  if (type == "previous" && makeColumn){
    column.className = "prev-column";
    var prevButton = document.createElement("button");
    prevButton.className = "book-button-" + queryID;
    prevButton.insertAdjacentHTML('afterbegin', "Previous");
    prevButton.addEventListener("click", function () {
      getBooksFromButton('books.previous', queryID)
    });
    column.appendChild(prevButton);

  } else if (type == "more" && makeColumn) {
    column.className = "more-column";
    var moreButton = document.createElement("button");
    moreButton.className = "book-button-" + queryID;
    moreButton.insertAdjacentHTML('afterbegin', "More");
    moreButton.addEventListener("click", function () {
      getBooksFromButton('books.more', queryID)
    });
    column.appendChild(moreButton);
  }
  return column;
}

/**
 * This function creates a <td></td> element containing a page count
 * for the current page of results
 *
 * @param indices Indices object
 * @return pageText of <td></td> element
 */
function createPageColumn(indices) {
  var totalPages = indices.totalPages;
  var currentPage = indices.currentPage;
  const pageColumn = document.createElement('td');
  pageColumn.className = "page-count";
  pageColumn.innerHTML = 'Page ' + currentPage + ' / ' + totalPages;
  return pageColumn;
}

/**
 * This function creates a row <tr></tr> element containing information from the
 * parameter book object to be added to the book table
 *
 * @param book Book object
 * @param queryID appropriate queryID for results in this element
 * @return bookRow element to be added to table
 */
function createBookRow(book, queryID) {
  const bookRow = document.createElement('tr');
  bookRow.className = "book-row";

  const picColumn = createPictureColumn(book);
  const infoColumn = createInfoColumn(book, queryID);
  const linkColumn = createLinkColumn(book, queryID);

  bookRow.appendChild(picColumn);
  bookRow.appendChild(infoColumn);
  bookRow.appendChild(linkColumn);
  return bookRow;
}

/**
 * This function creates a <td></td> element containing thumbnail link
 * of parameter book object
 *
 * @param book Book object
 * @return thumbnailText of <td></td> element
 */
function createPictureColumn(book) {
  const picColumn = document.createElement('td');
  picColumn.className = "book-thumbnail";

  if (book.thumbnailLink){
    picColumn.innerHTML = '<img class = "thumbnail" alt="Thumbnail." src="' + book.thumbnailLink + '">';
  } else {
    picColumn.innerHTML = '<img class = "thumbnail" alt="No Image Available" src="' + book.thumbnailLink + '">';
  }
  return picColumn;
}

/**
 * This function creates a <td></td> element containing information 
 * about the parameter book object
 *
 * @param book Book object
 * @param queryID appropriate queryID for results in this element
 * @return infoText of <td></td> element
 */
function createInfoColumn(book, queryID) {
  const infoColumn = document.createElement('td');
  infoColumn.className = "book-info";
  var titleHTML = '<b>' + book.title +'</b>';
  if (book.authors || book.publishedDate || book.averageRating){
      var infoHTML = '<p class = "book">';
      if (book.authors){
        infoHTML += book.authors +'<br>';
      }
      if (book.publishedDate){
        infoHTML += book.publishedDate +'<br>';
      }
      if (book.averageRating){
        infoHTML += 'Rating: ' + book.averageRating +'<br>';
      }
      infoHTML += '</p>';
      titleHTML += infoHTML;
  }
  infoColumn.insertAdjacentHTML('afterbegin', titleHTML);

  if (book.description){
      var descriptionButton = document.createElement("button");
      descriptionButton.className = "book-button-" + queryID;
      descriptionButton.insertAdjacentHTML('afterbegin', "Description");
      descriptionButton.addEventListener("click", function () {
        getBookInformation('books.description', book.order, queryID);
      });
      infoColumn.appendChild(descriptionButton);
  }
  if (book.embeddable == true && book.isbn){
      var previewButton = document.createElement("button");
      previewButton.className = "book-button-" + queryID;
      previewButton.insertAdjacentHTML('afterbegin', "Preview");
      previewButton.addEventListener("click", function () {
        getBookInformation('books.preview', book.order, queryID);
      });
      infoColumn.appendChild(previewButton);
  }
  // TODO: if user is logged in:
      var likeButton = document.createElement("button");
      likeButton.className = "book-button-like-" + book.order + "-" + queryID;
      var unlikeHeart = '\u2661';
      var likeHeart = '\u2764\uFE0F';
      if (book.isLiked) {
        likeButton.textContent = likeHeart;
      } else {
        likeButton.textContent = unlikeHeart;
      }
      likeButton.addEventListener("click", function () {
          const status = likeButton.textContent;
          if(status == likeHeart) {
            likeButton.textContent = unlikeHeart;
            handleBookLiked('unlike', book.order, queryID);
          } else {
            likeButton.textContent = likeHeart;
            handleBookLiked('like', book.order, queryID);
          }
      });
      infoColumn.appendChild(likeButton);
  return infoColumn;
}

/**
 * This function creates a <td></td> element containing info and 
 * buying links of parameter book object
 *
 * @param book Book object
 * @param queryID appropriate queryID for results in this element
 * @return linkText of <td></td> element
 */
function createLinkColumn(book, queryID) {
  const linkColumn = document.createElement('td');
  linkColumn.className = "book-links";
  const paragraph = document.createElement('p');
  paragraph.className = "book";

  if (book.infoLink){
    redirectLogo = '<img class = "redirect-logo" alt="Redirect" src= "images/redirect.png" >';
    linkHTML = '<a class = "book-link"  target="_blank" href = "' + book.infoLink + '">' + redirectLogo + 'Go to Page</a><br>';
    paragraph.insertAdjacentHTML('afterbegin', linkHTML);
  }
  addLogo = '<img class = "redirect-logo" alt="Redirect" src= "images/redirect.png" >';
  paragraph.insertAdjacentHTML('beforeend', addLogo);
  
  // TODO: only show up if user is logged in 
  var libraryLink = document.createElement('a');
  libraryLink.className = "add-link";
  libraryLink.insertAdjacentHTML('afterbegin', "Add to My Library");
  libraryLink.addEventListener("click", function () {
    getBookshelfNamesFromButton('books.add', book.order, queryID);
  });
  paragraph.appendChild(libraryLink);
  
  if (queryID.includes("-shelf")) {
    deleteLogo = '<img class = "redirect-logo" alt="Redirect" src= "images/redirect.png" >';
    paragraph.insertAdjacentHTML('beforeend', "<br>" + deleteLogo);

    var deleteLink = document.createElement('a');
    deleteLink.className = "delete-link";
    deleteLink.insertAdjacentHTML('afterbegin', "Remove from shelf");
    deleteLink.addEventListener("click", function () {
      editBookshelf('books.delete', "", book.order, queryID);
    });
    paragraph.appendChild(deleteLink);
  }
  

  linkColumn.appendChild(paragraph);
  return linkColumn;
}

/**
 * This function updates the scroll of content object to 
 * be set at the top of the book table returned by the assistant 
 *
 * @param queryID ID of assistant response to scroll to
 */
function updateBookScroll(queryID) {
  var recentResponse = document.getElementsByClassName("assistant-side-" + queryID)[document.getElementsByClassName("assistant-side-" + queryID).length - 1];
  var topPos = recentResponse.offsetTop;
  var element = document.getElementById("content");
  element.scrollTop = topPos;
}

/**
 * Displays description of the book added to the user's bookshelf and 
 * fulfilment text from the stream from Dialogflow
 *
 * @param stream output object stream from Dialogflow
 * @param bookshelfName name of bookshelf the book was added to
 */
function displayBookAdded(stream, bookshelfName) {
  var outputAsJson = JSON.parse(stream);
  if (outputAsJson.display) {
    clearPreviousDisplay(outputAsJson.redirect);
    placeBooksUserInput(outputAsJson.userInput, "convo-container", outputAsJson.redirect);
    placeBooksFulfillment(outputAsJson.fulfillmentText, outputAsJson.redirect);
    infoContainer = createBookInfoContainer(outputAsJson.display, outputAsJson.intent, outputAsJson.redirect, bookshelfName);
    placeBookDisplay(infoContainer, "convo-container", outputAsJson.redirect);
    if (outputAsJson.intent.includes("preview")) {
      loadPreview(outputAsJson.display);
    }
  } else {
    placeFulfillmentResponse(outputAsJson.fulfillmentText);
  }
  outputAudio(stream);
}

/**
 * Creates an information container containing infoType parameter
 * (either "description" or "preview") based on bookResult JSON from
 * Dialogflow
 *
 * @param bookResult JSON Book returned from Dialogflow
 * @param intent String specifying intent 
 * @param queryID appropriate queryID for results in this element
 * @param intent String specifying bookshelf current book is being added to 
 * @return infoDiv div element containing information table
 */
function createBookInfoContainer(bookResult, intent, queryID, bookshelfName){
  var book = JSON.parse(bookResult);

  infoDiv = document.createElement("div"); 
  infoDiv.className = "book-div";
  infoTable = document.createElement("table"); 
  infoTable.className = "book-table";

  infoTable.appendChild(createInfoRow(book, intent));
  infoTable.appendChild(createBookRow(book, queryID));
  const footerRow = createInfoFooter(queryID);
  if (bookshelfName) {
    footerRow.insertAdjacentHTML("beforeend", "<td></td>");
    const bookshelfCol = document.createElement('td');
    bookshelfCol.className = ("more-column");

    var shelfButton = document.createElement("button");
    shelfButton.className = "book-button-" + queryID;
    shelfButton.insertAdjacentHTML('afterbegin', "Go to " + bookshelfName);
    shelfButton.addEventListener("click", function () {
        goToBookshelf('books.library', bookshelfName);
    });
    bookshelfCol.appendChild(shelfButton);
    footerRow.appendChild(bookshelfCol);
  }
  infoTable.appendChild(footerRow);
  infoDiv.appendChild(infoTable);

  return infoDiv;
}

/**
 * Creates an information row containing corresponding displays based on 
 * parameter specifying the type of information from parameter bookResult
 * (either "description" or "preview") 
 *
 * @param bookResult JSON Book returned from Dialogflow
 * @param intent String specifying intent
 * @return infoRow <tr></tr> element for info table
 */
function createInfoRow(book, intent){
  infoRow = document.createElement('tr');
  infoCol = document.createElement('td');
  infoCol.colSpan = "3";
  infoRow.className = "book-row";

  if (intent.includes("books.description")) {
    infoCol.innerHTML = '<b> Description </b><br> <p class = "description">' + book.description + '</p><hr>';
  }
  else if (intent.includes("books.preview")) {
    infoCol.innerHTML = '<b> Preview </b><br>';
    infoCol.appendChild(getViewerDiv());
    infoCol.insertAdjacentHTML('beforeend', '<hr>');
  }
  infoRow.appendChild(infoCol);
  return infoRow;
}

/**
 * Creates a footer row for information table that includes a "Go Back"
 * button which will return users to the results display
 *
 * @param queryID appropriate queryID for results in this element
 * @return footerRow <tr></tr> element for info table
 */
function createInfoFooter(queryID){
  const footerRow = document.createElement('tr');
  footerRow.className = "book-row";
  const footerCol = document.createElement('td');
  
  var backButton = document.createElement("button");
  backButton.className = "book-button-" + queryID;
  backButton.insertAdjacentHTML('afterbegin', "Go Back");
  backButton.addEventListener("click", function () {
    getBooksFromButton('books.results', queryID);
  });
  footerCol.appendChild(backButton);
  footerRow.appendChild(footerCol);
  return footerRow;
}

/**
 * Creates an div element of class "book-viewer" to load embedded
 * book preview into
 * button which will return users to the results display
 *
 * @return viewerDiv <div class = "book-viewer"> </div> element
 */
function getViewerDiv() {
  viewerDiv = document.createElement('div');
  viewerDiv.className = "book-viewer";
  return viewerDiv;
}

/**
 * Retrieves a DefaultViewer object from Google Books API that displays
 * embedded preview of the bookResult object parameter in the most recent
 * book-viewer container
 */
function loadPreview(bookResult) {
  var book = JSON.parse(bookResult);
  var viewer = new google.books.DefaultViewer(document.getElementsByClassName('book-viewer')[document.getElementsByClassName("book-viewer").length - 1]);
  var isbnNum = 'ISBN:' + book.isbn;
  viewer.load(isbnNum, alertNotFound);
  viewer.resize();
}

function alertNotFound() {
  alert("could not embed the book!");
}

/**
 * Displays bookshelf names when the user requests to add a book
 * to their library
 *
 * @param stream output object stream from Dialogflow
 * @param bookIndex Book number to display
 * @param queryID queryID for div that triggered button
 */
function displayBookshelvesToAdd(stream, bookIndex, queryID) {
  var outputAsJson = JSON.parse(stream);
  clearPreviousDisplay(queryID);
  placeBooksUserInput(outputAsJson.userInput, "convo-container", queryID);
  placeBooksFulfillment(outputAsJson.fulfillmentText, queryID);
  bookShelfAddContainer = createBookshelfAddContainer(outputAsJson.display, bookIndex, queryID);
  placeBookDisplay(bookShelfAddContainer, "convo-container", queryID);
  outputAudio(stream);
}

/**
 * This function creates a Book container containing a 
 * <table></table> element with information about Bookshelf names from the 
 * json bookResults parameter, along with the requested book number and queryID
 * indicating which volume to add to bookshelf
 *
 * @param bookResults json ArrayList<String> bookshelf names
 * @param bookIndex Book number to display
 * @param queryID queryID for div that triggered button
 * @return booksDiv element containing a book results table 
 */
function createBookshelfAddContainer(bookResults, bookIndex, queryID) {
  var bookshelfList = JSON.parse(bookResults);
  var booksDiv = document.createElement("div"); 
  booksDiv.className = "book-div";
  var bookTable = document.createElement("table"); 
  bookTable.className = "book-table";
  bookshelfList.forEach((bookName) => {
    bookTable.appendChild(createBookShelfAddRow(bookName, bookIndex, queryID));
  });
  bookTable.appendChild(createInfoFooter(queryID));
  booksDiv.appendChild(bookTable);
  return booksDiv;
}

/**
 * This function creates a row <tr></tr> element containing information from the
 * parameter bookshelf name to be added to the book table, along with the 
 * neccessary metadata for book to add to the bookshelf on click
 *
 * @param bookshelfName name of bookshelf
 * @param bookIndex Book number to display
 * @param queryID queryID for div that triggered button
 * @return bookRow element to be added to table
 */
function createBookShelfAddRow(bookshelfName, bookIndex, queryID) {
  const bookRow = document.createElement('tr');
  bookRow.className = "book-row";

  const bookshelfColumn = document.createElement('td');
  bookshelfColumn.className = "bookshelf-name";

  var bookshelfButton = document.createElement("button");
  bookshelfButton.className = "bookshelf-button";
  bookshelfButton.insertAdjacentHTML('afterbegin', bookshelfName);
  bookshelfButton.addEventListener("click", function () {
    editBookshelf('books.add', bookshelfName, bookIndex, queryID);
  });
  bookshelfColumn.appendChild(bookshelfButton);
  bookRow.appendChild(bookshelfColumn);
  return bookRow;
}

/**
 * This function clears the last container specified by parameter
 *
 * @param className class name of container to be cleared
 */
function clearLastDiv(className) {
  lastElement = document.getElementsByClassName(className)[document.getElementsByClassName(className).length - 1];
  if (lastElement) {
    lastElement.parentNode.removeChild(lastElement);
  }
}

/**
 * This function clears the last comment made by either the user
 * or the assistant side
 *
 * @param className class name of comment to be cleared
 */
function clearLastComment(className) {
  lastElement = document.getElementsByClassName(className)[document.getElementsByClassName(className).length - 1];
  if (lastElement) {
    parentContainer = lastElement.parentNode;
    parentContainer.removeChild(lastElement);
    parentContainer.parentNode.removeChild(parentContainer);
  }
}

/**
 * This function clears the last comment made by the user
 * and the assistant along with the previous books display that 
 * matches the queryID
 *
 * @param queryID ID of results for query to be cleared
 */
function clearPreviousDisplay(queryID) {
  clearLastComment("user-side-" + queryID);
  clearLastComment("assistant-side-" + queryID);
  clearLastDiv(queryID);
}