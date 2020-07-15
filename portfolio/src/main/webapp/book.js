/**
 * This function places a book div element in the container specified by the 
 * parameter and updates the scroll to the top of the table displayed
 *
 * @param bookDiv element containing book table
 * @param query container to place book table in
 */
function placeBookDisplay(bookDiv, container) {
  var container = document.getElementsByName(container)[0];
  container.appendChild(bookDiv);
  updateBookScroll();
}

/**
 * This function creates a Book container containing a 
 * <table></table> element with information about Book objects from the 
 * json bookResults parameter
 *
 * @param bookResults json ArrayList<Book> objects
 * @return booksDiv element containing a book results table 
 */
function createBookContainer(bookResults) {
    var booksList = JSON.parse(bookResults);
    var booksDiv = document.createElement("div"); 
    booksDiv.className = "book-div";
    var bookTable = document.createElement("table"); 
    bookTable.className = "book-table";
    booksList.forEach((book) => {
      bookTable.appendChild(createBookRow(book));
    });
    bookTable.appendChild(createTableFooter());
    booksDiv.appendChild(bookTable);
    return booksDiv;
}

/**
 * This function creates a table footer <tr></tr> element containing current 
 * page index and previous and next buttons (if applicable)
 *
 * @return footerRow element to be added to the bottom of table
 */
function createTableFooter() {
  const footerRow = document.createElement('tr');
  footerRow.className = "book-row";
  fetch('/book').then(response => response.json()).then((indices) => {
    const prevColumn = createPrevColumn(indices);
    const pageColumn = createPageColumn(indices);
    const moreColumn = createMoreColumn(indices);
    footerRow.innerHTML = prevColumn + pageColumn + moreColumn;
  });
  return footerRow;
}

/**
 * This function creates a <td></td> element containing a previous button
 * if there are previous results
 *
 * @param indices Indices object
 * @return prevText of <td></td> element
 */
function createPrevColumn(indices) {
  var hasPrev = indices.hasPrev;

  const prevColumn = document.createElement('td');
  prevColumn.className = "prev-column";

  if (hasPrev){
    htmlString = '<button class="book-button" onclick="getBooksFromButton(\'Show previous\')"> Previous </button>';
    prevColumn.insertAdjacentHTML('afterbegin', htmlString);
  }
  return prevColumn.outerHTML;
}

/**
 * This function creates a <td></td> element containing a more button
 * if there are more results
 *
 * @param indices Indices object
 * @return moreText of <td></td> element
 */
function createMoreColumn(indices) {
  var hasMore = indices.hasMore;
  const moreColumn = document.createElement('td');
  moreColumn.className = "more-column";
  if (hasMore){
    htmlString = '<button class="book-button" onclick="getBooksFromButton(\'Show more\')"> More </button>';
    moreColumn.insertAdjacentHTML('afterbegin', htmlString);
  }
  return moreColumn.outerHTML;
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
  return pageColumn.outerHTML;
}

/**
 * This function creates a row <tr></tr> element containing information from the
 * parameter book object to be added to the book table
 *
 * @param book Book object
 * @return bookRow element to be added to table
 */
function createBookRow(book) {
  const bookRow = document.createElement('tr');
  bookRow.className = "book-row";

  const picColumn = createPictureColumn(book);
  const infoColumn = createInfoColumn(book);
  const linkColumn = createLinkColumn(book);

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
 * @return infoText of <td></td> element
 */
function createInfoColumn(book) {
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
      descriptionButton.className = "book-button";
      descriptionButton.insertAdjacentHTML('afterbegin', "Description");
      descriptionButton.addEventListener("click", function () {
        var request = 'description ' + book.order;
        getBookInformation(request);
      });
      infoColumn.appendChild(descriptionButton);
  }
  if (book.embeddable == true && book.isbn){
      var previewButton = document.createElement("button");
      previewButton.className = "book-button";
      previewButton.insertAdjacentHTML('afterbegin', "Preview");
      previewButton.addEventListener("click", function () {
        var request = 'preview ' + book.order;
        getBookInformation(request);
      });
      infoColumn.appendChild(previewButton);
  }
  return infoColumn;
}

/**
 * This function creates a <td></td> element containing info and 
 * buying links of parameter book object
 *
 * @param book Book object
 * @return linkText of <td></td> element
 */
function createLinkColumn(book) {
  const linkColumn = document.createElement('td');
  linkColumn.className = "book-links";

  if (book.infoLink || book.buyLink){
      linkHTML = '<p class = "book">';
      if (book.buyLink){
          linkHTML += '<a class = "book-link"  target="_blank" href = "' + book.buyLink + '">Buy It</a><br>';
      }
      if (book.infoLink){
          linkHTML += '<a class = "book-link"  target="_blank" href = "' + book.infoLink + '">Go to Page</a>';
      }
      linkHTML += '</p>';
  }
  linkColumn.innerHTML = linkHTML;
  return linkColumn;
}

/**
 * This function updates the scroll of content object to 
 * be set at the top of the book table returned by the assistant 
 */
function updateBookScroll() {
  var recentResponse = document.getElementsByClassName("assistant-side")[document.getElementsByClassName("assistant-side").length - 1];
  var topPos = recentResponse.offsetTop;
  var element = document.getElementById("content");
  element.scrollTop = topPos;
}

/**
 * Displays book description and fulfilment text from the stream
 * from Dialogflow
 *
 * @param stream output object stream from Dialogflow
 */
function displayBookInfo(stream) {
  var outputAsJson = JSON.parse(stream);
  placeFulfillmentResponse(outputAsJson.fulfillmentText);

  if (outputAsJson.display) {
      if (outputAsJson.intent.includes("description")){
        descriptionContainer = createBookInfoContainer(outputAsJson.display, "description");
        placeBookDisplay(descriptionContainer, "convo-container");
      } else if (outputAsJson.intent.includes("preview")) {
        previewContainer = createBookInfoContainer(outputAsJson.display, "preview");
        placeBookDisplay(previewContainer, "convo-container");
        loadPreview(outputAsJson.display);
      }
  }
  outputAudio(stream);
}

/**
 * Creates an information container containing infoType parameter
 * (either "description" or "preview") based on bookResult JSON from
 * Dialogflow
 *
 * @param bookResult JSON Book returned from Dialogflow
 * @param infoType String specifying type of info to display
 * @return infoDiv div element containing information table
 */
function createBookInfoContainer(bookResult, infoType){
  var book = JSON.parse(bookResult);

  infoDiv = document.createElement("div"); 
  infoDiv.className = "book-div";
  infoTable = document.createElement("table"); 
  infoTable.className = "book-table";

  infoTable.appendChild(createInfoRow(book, infoType));
  infoTable.appendChild(createBookRow(book));
  infoTable.appendChild(createInfoFooter());
  infoDiv.appendChild(infoTable);

  return infoDiv;
}

/**
 * Creates an information row containing corresponding displays based on 
 * parameter specifying the type of information from parameter bookResult
 * (either "description" or "preview") 
 *
 * @param bookResult JSON Book returned from Dialogflow
 * @param infoType String specifying type of info to display
 * @return infoRow <tr></tr> element for info table
 */
function createInfoRow(book, infoType){
  infoRow = document.createElement('tr');
  infoCol = document.createElement('td');
  infoCol.colSpan = "3";
  infoRow.className = "book-row";

  if (infoType == "description") {
    infoCol.innerHTML = '<b> Description </b><br> <p class = "description">' + book.description + '</p><hr>';
  }
  else if (infoType == "preview") {
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
 * @param bookResult JSON Book returned from Dialogflow
 * @param infoType String specifying type of info to display
 * @return footerRow <tr></tr> element for info table
 */
function createInfoFooter(){
  const footerRow = document.createElement('tr');
  footerRow.className = "book-row";
  
  htmlString = '<button class="book-button" onclick="getBooksFromButton(\'back to results\')"> Go Back </button>';
  footerRow.insertAdjacentHTML('afterbegin', htmlString);
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
 * Displays book results without user input text when one of the  
 * buttons in the display has been pressed, triggering a book results
 * display 
 *
 * @param stream output object stream from Dialogflow
 */
function displayBooksFromButton(stream) {
  var outputAsJson = JSON.parse(stream);
  placeFulfillmentResponse(outputAsJson.fulfillmentText);
  if (outputAsJson.intent.includes("books.more") ||
        outputAsJson.intent.includes("books.previous") ||
        outputAsJson.intent.includes("books.results")){
    bookContainer = createBookContainer(outputAsJson.display);
    placeBookDisplay(bookContainer, "convo-container");
  }
  outputAudio(stream);
}