function placeBookDisplay(bookDiv, container) {
  var container = document.getElementsByName(container)[0];
  container.appendChild(bookDiv);
  updateBookScroll();
}

function updateBookScroll() {
  var recentResponse = document.getElementsByClassName("assistant-side")[document.getElementsByClassName("assistant-side").length - 1];
  var topPos = recentResponse.offsetTop;
  var element = document.getElementById("content");
  element.scrollTop = topPos;
}

function createBookContainer(bookResults) {
    var booksList = JSON.parse(bookResults);
    var booksDiv = document.createElement("div"); 
    booksDiv.className = "book-div";
    var bookTable = document.createElement("table"); 
    bookTable.className = "book-table";
    booksList.forEach((book) => {
      bookTable.appendChild(createBookRow(book));
    })
    booksDiv.innerHTML = bookTable.outerHTML;
    return booksDiv;
}

function createBookRow(book) {
  const bookRow = document.createElement('tr');
  const picColumn = document.createElement('td');
  const infoColumn = document.createElement('td');
  const linkColumn = document.createElement('td');

  bookRow.className = "book-row";
  picColumn.className = "book-thumbnail";
  infoColumn.className = "book-info";
  linkColumn.className = "book-links";

  if (book.thumbnailLink){
    picColumn.innerHTML = '<img class = "thumbnail" alt="Thumbnail." src="' + book.thumbnailLink + '">';
  } else {
    picColumn.innerHTML = '<img class = "thumbnail" alt="No Image Available" src="' + book.thumbnailLink + '">';
  }
  
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

  if (book.infoLink || book.buyLink){
      linkHTML = '<p class = "book">';
      if (book.buyLink){
          linkHTML += '<a class = "book-link" href = "' + book.buyLink + '">Buy It</a><br>';
      }
      if (book.infoLink){
          linkHTML += '<a class = "book-link" href = "' + book.infoLink + '">Go to Page</a>';
      }
      linkHTML += '</p>';
      titleHTML += linkHTML;
  }
  linkColumn.innerHTML = linkHTML;

  thumbnailText = picColumn.outerHTML;
  infoText = infoColumn.outerHTML;
  linkText = linkColumn.outerHTML;

  bookRow.innerHTML = thumbnailText + infoText + linkText;
  return bookRow;
}