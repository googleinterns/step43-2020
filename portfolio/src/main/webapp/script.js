// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
 
/**
 * Starts listening to user input and fetches user input string.
 * Defines sessionId global variable and function to call when window closes.
 */
 
const mainSection = document.querySelector('.main-controls');
const formContainer = document.getElementsByName('input-form')[0];
const textInputContainer = document.getElementById("text-input");
var sessionId = "";
var queryNumber = 0;
window.onbeforeunload = deleteSessionInformation;
 
var pastCommands = loadCommands();
var commandIndex = pastCommands.length;
var unsentLastCommand;

var sessionId = "";
var queryNumber = 0;
window.onbeforeunload = deleteSessionInformation;

/**
* Function triggered with each character typed in the text input container that handles 
* submitting the text input on the RETURN key, getting the command used before with the 
* UP key, and getting the command used after with the DOWN key (same basic functionality 
* as in terminal).
* 
* @param e The key pressed.
*/
formContainer.onkeyup = function(e){
  if(e.keyCode == 13 && !isEmptyString(textInputContainer.value)) { //return key and non-empty input
    getResponseFromText();
  } else if(e.keyCode == 38) { //up arrow key
    if (commandIndex == 0) {
      return;
    }
    if (commandIndex == pastCommands.length) {
      unsentLastCommand = textInputContainer.value;
    }
    commandIndex --;
    textInputContainer.value = pastCommands[commandIndex].trim();
  } else if(e.keyCode == 40) { //down arrow key
    if (commandIndex == pastCommands.length) {
      return;
    }
    commandIndex ++;
    if (commandIndex == pastCommands.length) {
      textInputContainer.value = (unsentLastCommand == null) ? "" : unsentLastCommand;
    } else {
      textInputContainer.value = pastCommands[commandIndex].trim();
    }
  }
};

window.onresize = function() {
  canvas.width = mainSection.offsetWidth;
}
 
window.onresize();

/**
* Retrives the saved language from session storage or English as default.
*/
function getLanguage() {
  var language = window.sessionStorage.getItem("language");
  language = language == null ? "English" : language;
  return language;
}

/**
* Backend call to speech-to-text that handles streaming inputs while the user 
* is talking and converts them to text.
* 
* @param blob Mini audio file containing a subset of the user's entire speech.
*/
function getAudioStream(blob) {
  fetch('/audio-stream' + '?language=' + getLanguage(), {
    method: 'POST',
    body: blob
  }).then(response => response.text()).then(stream => {
    streamingContainer.innerHTML = "";
    stream = (stream.includes(null)) ? "" : stream;
    placeUserInput(stream + "...", "streaming");
  });
}

/**
* Backend call to dialogflow that handles recognizing the user's intent and 
* accomplishing the necessary backend fulfillment to carry out the user's request.
* Creates an audio output and handles making any displays that are necessary.
* 
* @param blob Audio file containing the user's entire speech from start to stop recording.
*/
function getResponseFromAudio(blob) {
  const formData = new FormData();
  formData.append('audio-file', blob);
  fetch('/audio-input' + '?language=' + getLanguage() + '&session-id=' + sessionId, {
    method: 'POST',
    body: blob
  }).then(response => response.text()).then(stream => displayResponse(stream));
}

/**
* Backend call to dialogflow that takes the user's text input from the form container and 
* accomplishes the necessary backend fulfillment to carry out the user's request.
* Creates an audio output and handles making any displays that are necessary.
*/
function getResponseFromText(){
  var input = textInputContainer.value;
  saveCommand(input);
  unsentLastCommand = null;
  fetch('/text-input?request-input=' + input + '&language=' + getLanguage() + '&session-id=' + sessionId, {
      method: 'POST'
  }).then(response => response.text()).then(stream => displayResponse(stream));
  formContainer.reset(); 
}

/**
* Backend call to UserServices to determine if user is logged in and display a personalized greeting.
*/
function authSetup() {
  fetch("/auth").then((response) => response.json()).then((displayText) => {
    var authContainer = document.getElementsByClassName("auth-link")[0];
    authContainer.innerHTML = "<a class=\"link\" href=\"" + displayText.authText + "\">" + displayText.logButton + "</a>";
    updateName(displayText.displayName);
    getSessionID();
    // Clears any stored information in Datastore for this session upon loading
    deleteSessionInformation();
  });
}

/**
* Saves each command made by the user into a string in session storage so that the user 
* can use the UP/DOWN arrows to access recently used commands.
*
* @param text The string command typed by the user.
*/
function saveCommand(text) {
  if (isEmptyString(text)) {
    return;
  }
  var commandHistory = window.sessionStorage.getItem("commandHistory");
  if (commandHistory == null) {
    commandHistory = text;
  } else {
    commandHistory += text;
  }
  pastCommands.push(text);
  commandIndex = pastCommands.length;
  window.sessionStorage.setItem("commandHistory", commandHistory);
}

/**
* Loads all past commands from the previous session so that user initially
* has access to their recent past commands.
*/
function loadCommands() {
  var commandHistory = window.sessionStorage.getItem("commandHistory");
  if (commandHistory == null) {
    return [];
  }
  commandHistory = commandHistory.trim();
  return commandHistory.split("\n");
}

function isEmptyString(text) {
  return text == null || text.trim() === "";
}

function isEmptyString(text) {
  return text == null || text.trim() === "";
}

/**
 * Retrieves Output object created by BookAgent for specified intent 
 * triggered by a button the display 
 * 
 * @param intent name of book intent
 * @param queryID queryID for div that triggered button
 */
function getBooksFromButton(intent, queryID){
  fetch('/book-agent?intent=' + intent + '&language=' + getLanguage() + '&session-id=' + sessionId + 
    '&query-id=' + queryID, {
      method: 'POST'
  }).then(response => response.text()).then(stream =>displayBooksFromButton(stream));
}

/**
 * Retrieves Output object created by BookAgent for specified information intent 
 * triggered by a button the display. Number parameter specifies the Book object 
 * number to retrieve.
 * 
 * @param intent name of book intent
 * @param number index of book to retrieve information for
 * @param queryID queryID for div that triggered button for information
 */
function getBookInformation(intent, number, queryID){
  fetch('/book-agent?intent=' + intent + '&language=' + getLanguage() + '&session-id=' + sessionId +
    '&number=' + number + '&query-id=' + queryID, {
      method: 'POST'
  }).then(response => response.text()).then(stream =>displayBookInfo(stream));
}

function getBookshelfInformation(intent, bookshelfName) {
  fetch('/book-agent?intent=' + intent + '&language=' + getLanguage() + '&session-id=' + sessionId +
    '&bookshelf=' + bookshelfName, {
      method: 'POST'
  }).then(response => response.text()).then(stream =>displayBooksFromButton(stream));
}
/**
 * Returns userID, if user is logged in, or guestID for the session otherwise
 */
function getSessionID(){
  fetch('/id').then(response => response.text()).then((id) => {
      window.sessionId = id;
  });
}

/**
 * Deletes all stored Entitys that match current sessionID before
 * users close the page
 */
function deleteSessionInformation(){
  fetch('/id' + '?session-id=' + sessionId, {
      method: 'POST'
  }).then(response => response.text()).then(() => {
      console.log('Deleted comments for ' + sessionId)
  });
  return null;
}
