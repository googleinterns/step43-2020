// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
 
/**
 * Starts listening to user input and fetches user input string
 */
 
 
const record = document.querySelector('.record');
const stop = document.querySelector('.stop');
const soundClips = document.querySelector('.sound-clips');
const canvas = document.querySelector('.visualizer');
const mainSection = document.querySelector('.main-controls');
const streamingContainer = document.getElementsByName('streaming')[0];
 
// disable stop button while not recording
 
stop.disabled = true;
 
record.addEventListener("click", startRecording);
stop.addEventListener("click", stopRecording);

var streamingStarted;
 
function startRecording() {
    console.log("recordButton clicked");
    streamingContainer.style.display = "initial";
    placeUserInput("...", "streaming");
    var constraints = { audio: true, video:false }
 
    // Disable the record button until we get a success or fail from getUserMedia() 
    record.disabled = true;
    stop.disabled = false;
    record.style.background = "#DB4437";
 
    navigator.mediaDevices.getUserMedia(constraints).then(function(stream) {
        console.log("getUserMedia() success, stream created, initializing Recorder.js ...");
 
        /*
            create an audio context after getUserMedia is called
            sampleRate might change after getUserMedia is called, like it does on macOS when recording through AirPods
            the sampleRate defaults to the one set in your OS for your playback device
        */
        audioContext = new AudioContext();
 
        /*  assign to gumStream for later use  */
        gumStream = stream;
        
        /* use the stream */
        input = audioContext.createMediaStreamSource(stream);
 
        /* 
            Create the Recorder object and configure to record mono sound (1 channel)
            Recording 2 channels  will double the file size
        */
        rec = new Recorder(input,{numChannels:1})
 
        //start the recording process
        rec.record()
        console.log("Recording started");

        streamingStarted = setInterval(streamAudio, 1000);
 
    }).catch(function(err) {
        //enable the record button if getUserMedia() fails
        record.disabled = false;
        stop.disabled = true;
    });
}

function streamAudio() {
  rec.stop();
  rec.exportWAV(getAudioStream);
  rec.record();
}
 
function stopRecording() {
    console.log("stopButton clicked");
    clearInterval(streamingStarted);
 
    //disable the stop button, enable the record too allow for new recordings
    stop.disabled = true;
    record.disabled = false;
    record.style.background = "";
    record.style.color = "";
    
    //tell the recorder to stop the recording
    rec.stop();
 
    //stop microphone access
    gumStream.getAudioTracks()[0].stop();
 
    //create the wav blob and pass it on to createDownloadLink
    rec.exportWAV(getResponseFromAudio);
}
 
// visualiser setup - create web audio api context and canvas
 
let audioCtx;
const canvasCtx = canvas.getContext("2d");
 
//main block for doing the audio recording
 
if (navigator.mediaDevices.getUserMedia) { 
  const constraints = { audio: { sampleSize: 16, channelCount: 1, sampleRate: 16000 } }; 
  
  let onSuccess = function(stream) {
    const mediaRecorder = new MediaRecorder(stream);
    visualize(stream);
  }
 
  let onError = function(err) {
    console.log('The following error occurred: ' + err);
  }
 
  navigator.mediaDevices.getUserMedia(constraints).then(onSuccess, onError);
 
} else {
   console.log('getUserMedia not supported on your browser!');
}
 
function visualize(stream) {
  if(!audioCtx) {
    audioCtx = new AudioContext();
  }
 
  const source = audioCtx.createMediaStreamSource(stream);
 
  const analyser = audioCtx.createAnalyser();
  analyser.fftSize = 2048;
  const bufferLength = analyser.frequencyBinCount;
  const dataArray = new Uint8Array(bufferLength);
 
  source.connect(analyser);
 
  draw()
 
  function draw() {
    const WIDTH = canvas.width
    const HEIGHT = canvas.height;
 
    requestAnimationFrame(draw);
 
    analyser.getByteTimeDomainData(dataArray);
 
    canvasCtx.fillStyle = 'rgba(42,42,42,0)';
    canvasCtx.fillRect(0, 0, WIDTH, HEIGHT);
    canvasCtx.clearRect(0, 0, WIDTH, HEIGHT);
 
 
    var gradient = canvasCtx.createLinearGradient(0, 0, WIDTH, 0);
    gradient.addColorStop("0", "#DB4437");
    gradient.addColorStop("0.33", "#F4B400");
    gradient.addColorStop("0.66", "#0F9D58");
    gradient.addColorStop("1.0", "#4285F4");
 
    canvas.lineWidth = 2;
    canvasCtx.strokeStyle = gradient;
 
    canvasCtx.beginPath();
 
    let sliceWidth = WIDTH * 1.0 / bufferLength;
    let x = 0;
 
 
    for(let i = 0; i < bufferLength; i++) {
 
      let v = dataArray[i] / 128.0;
      let y = v * HEIGHT/2;
 
      if(i === 0) {
        canvasCtx.moveTo(x, y);
      } else {
        canvasCtx.lineTo(x, y);
      }
 
      x += sliceWidth;
    }
 
    canvasCtx.lineTo(canvas.width, canvas.height/2);
    canvasCtx.stroke();
  }
}

window.onresize = function() {
  canvas.width = mainSection.offsetWidth;
}
 
window.onresize();

function getLanguage() {
  var language = window.sessionStorage.getItem("language");
  language = language == null ? "English" : language;
  return language;
}

function getAudioStream(blob) {
  fetch('/audio-stream' + '?language=' + getLanguage(), {
    method: 'POST',
    body: blob
  }).then(response => response.text()).then(stream => {
    streamingContainer.innerHTML = "";
    placeUserInput(stream + "...", "streaming");
  });
}
 
function getResponseFromAudio(blob) {
  const formData = new FormData();
  formData.append('audio-file', blob);
 
  fetch('/audio-input' + '?language=' + getLanguage(), {
    method: 'POST',
    body: blob
  }).then(response => response.text()).then(stream => displayResponse(stream));
}
 
function getResponseFromText(){
  var input = document.getElementById('text-input').value;
  fetch('/text-input?request-input=' + input + '&language=' + getLanguage(), {
      method: 'POST'
  }).then(response => response.text()).then(stream => displayResponse(stream));
 
  var frm = document.getElementsByName('input-form')[0];
  frm.reset(); 
}
 
function displayResponse(stream) {
  var outputAsJson = JSON.parse(stream);
  placeUserInput(outputAsJson.userInput, "convo-container");
  placeFulfillmentResponse(outputAsJson.fulfillmentText);
  outputAudio(stream);
}
 
function placeUserInput(text, container) {
  if (container == "convo-container") {
    streamingContainer.innerHTML = "";
    streamingContainer.style.display = "none";
  }
  if (text != " (null) "){
    var formattedInput = text.substring(0, 1).toUpperCase() + text.substring(1); 
    placeObjectContainer("<p>" + formattedInput + "</p>", "user-side", container);
  }
}
 
function placeFulfillmentResponse(text) {
  placeObjectContainer("<p>" + text + "</p>", "assistant-side", "convo-container");
  console.log(text);
  if (text.includes("Switching conversation language")) {
    window.sessionStorage.setItem("language", getLastWord(text));
  }
}

function getLastWord(words) {
    var split = words.split(/[ ]+/);
    console.log(split);
    return split[split.length - 1];
}
 
function placeDisplay(text) {
  placeObjectContainer(text, "media-display", "convo-container");
}

function placeObjectContainer(text, type, container) {
  var container = document.getElementsByName(container)[0];
  container.innerHTML += ("<div class='" + type + "'>" + text + "</div><br>")
  updateScroll();
}

function updateScroll() {
  var element = document.getElementById("content");
  element.scrollTop = element.scrollHeight;
}

function outputAudio(stream){
  var outputAsJson = JSON.parse(stream);
  getAudio(outputAsJson.byteStringToByteArray);

  if (outputAsJson.redirect != null){
    var aud = document.getElementById("sound-player");
    aud.onended = function() {
      sendRedirect(outputAsJson.redirect);
    };
  } else {
      var aud = document.getElementById("sound-player");
      aud.onended = function() {};
  }
}
 
function sendRedirect(URL){
  window.open(URL);
}
 
function getAudio(byteArray){
  var base64 = arrayBufferToBase64(byteArray);
  var audioURL = base64toURL(base64, "audio/mp3");
  play(audioURL);
}
 
function arrayBufferToBase64(buffer) {
  var binary = '';
  var bytes = new Uint8Array(buffer);
  var len = bytes.byteLength;
  for (var i = 0; i < len; i++) {
    binary += String.fromCharCode( bytes[ i ] );
  }
  return window.btoa(binary);
}
 
function base64toURL(b64Data, type){
  var audioURL = "data:" + type + ";base64," + b64Data;
  return audioURL;
}
 
function play(src) {
  var elem = document.getElementById('sound-player'),
      body = document.body;
 
  src = src.replace(/\s/g, '%20').replace(/\\/g, '/');
 
  if (!elem) {
    elem = document.createElement('audio');
    elem.src = src;
    elem.id = 'sound-player';
    elem.setAttribute('autoplay', '');
    elem.setAttribute('preload', 'auto');
    if (body) {
      body.appendChild(elem);
    }
  } else {
    if (elem.src !== src) {
      elem.src = src;
    } else {
      elem.play();
    }
  }
}
