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
 * Starts listening to user input and fetches user input string
 */
 
const mainSection = document.querySelector('.main-controls');
const formContainer = document.getElementsByName('input-form')[0];
const textInputContainer = document.getElementById("text-input");
 

formContainer.onkeyup = function(e){
  if(e.keyCode == 13 && textInputContainer.value.length != 0) { //return key and non-empty input
    getResponseFromText();
  }
};

window.onresize = function() {
  canvas.width = mainSection.offsetWidth;
}
 
window.onresize();

function getLanguage() {
  var language = window.sessionStorage.getItem("language");
  language = language == null ? "English" : language;
  return language;
}

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
 
function getResponseFromAudio(blob) {
  const formData = new FormData();
  formData.append('audio-file', blob);
 
  fetch('/audio-input' + '?language=' + getLanguage(), {
    method: 'POST',
    body: blob
  }).then(response => response.text()).then(stream => displayResponse(stream));
}
 
function getResponseFromText(){
  var input = textInputContainer.value;
  fetch('/text-input?request-input=' + input + '&language=' + getLanguage(), {
      method: 'POST'
  }).then(response => response.text()).then(stream => displayResponse(stream));
 
  formContainer.reset(); 
}

function sendRedirect(URL){
  window.open(URL);
}

function authSetup() {
  fetch("/auth").then((response) => response.json()).then((displayText) => {
    var authContainer = document.getElementsByClassName("auth-link")[0];
    authContainer.innerHTML = "<a class=\"link\" href=\"" + displayText.authText + "\">" + displayText.logButton + "</a>";
    updateName(displayText.displayName);
  });
}

function updateName(name) {
  var greetingContainer = document.getElementsByName("greeting")[0];
  greetingContainer.innerHTML = "<h1>Hi " + name + ", what can I help you with?</h1>";
}

function getBooksFromButton(request){
  fetch('/text-input?request-input=' + request + '&language=' + getLanguage(), {
      method: 'POST'
  }).then(response => response.text()).then(stream =>displayBooksFromButton(stream));
}

function getBookInformation(request){
  fetch('/text-input?request-input=' + request + '&language=' + getLanguage(), {
      method: 'POST'
  }).then(response => response.text()).then(stream =>displayBookInfo(stream));
}