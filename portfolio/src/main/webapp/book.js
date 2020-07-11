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
 * @param book Book object
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
    prevColumn.innerHTML = '<button class="book-button"> Previous </button>';
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
    moreColumn.innerHTML = '<button class="book-button"> More </button>';
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

  bookRow.innerHTML = picColumn + infoColumn + linkColumn;
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
  return picColumn.outerHTML;
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
  if (book.description){
      titleHTML += '<button class = "book-button"> Description </button>';
  }
  if (book.embeddable == true && book.isbn){
      titleHTML += '<button class = "book-button"> Preview </button>';
  }
  infoColumn.innerHTML = titleHTML;
  return infoColumn.outerHTML;
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
  return linkColumn.outerHTML;
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