const streamingContainer = document.getElementsByName('streaming')[0];

/**
* Creates all frontend display for user and assistant comments and specialized displays for each agent.
*
* @param stream JSON output from dialogflow containing all necessary display information.
*/
function displayResponse(stream) {
  var outputAsJson = JSON.parse(stream);
  if (!outputAsJson.intent.includes("books") || !outputAsJson.display) {
    placeUserInput(outputAsJson.userInput, "convo-container");
    placeFulfillmentResponse(outputAsJson.fulfillmentText);
  }
  if (outputAsJson.display) {
    if (outputAsJson.intent.includes("reminders.snooze")) {
      convoContainer = placeObjectContainer(outputAsJson.display, "media-display timer-display", "convo-container");
      var allTimers = document.getElementsByClassName("timer-display");
      if (existingTimer) {
        terminateTimer(allTimers[0]);
      }
      existingTimer = true;
    } else if (outputAsJson.intent.includes("name.user.change")) {
      updateName(outputAsJson.display);
    } else if (outputAsJson.intent.includes("maps.search")) {
      mapContainer = locationMap(outputAsJson.display);
      appendDisplay(mapContainer);
    } else if (outputAsJson.intent.includes("maps.find")) {
      if (moreButton) {
        moreButton.style.display = "none";
      }
      mapContainer = nearestPlacesMap(outputAsJson.display);
      appendDisplay(mapContainer);
    } else if (outputAsJson.intent.includes("books.search") ||
        outputAsJson.intent.includes("books.more") ||
        outputAsJson.intent.includes("books.previous") ||
        outputAsJson.intent.includes("books.results")) {
      if (!outputAsJson.intent.includes("books.search")){
        clearPreviousDisplay(outputAsJson.redirect);
      }
      placeBooksUserInput(outputAsJson.userInput, "convo-container", outputAsJson.redirect);
      placeBooksFulfillment(outputAsJson.fulfillmentText, outputAsJson.redirect);
      bookContainer = createBookContainer(outputAsJson.display, outputAsJson.redirect);
      placeBookDisplay(bookContainer, "convo-container", outputAsJson.redirect);

    } else if (outputAsJson.intent.includes("books.description") ||
        outputAsJson.intent.includes("books.preview")) {
      clearPreviousDisplay(outputAsJson.redirect);

      placeBooksUserInput(outputAsJson.userInput, "convo-container", outputAsJson.redirect);
      placeBooksFulfillment(outputAsJson.fulfillmentText, outputAsJson.redirect);
      infoContainer = createBookInfoContainer(outputAsJson.display, outputAsJson.intent, outputAsJson.redirect);
      placeBookDisplay(infoContainer, "convo-container", outputAsJson.redirect);

    } else if (outputAsJson.intent.includes("workout.find")) {
      workoutContainer = workoutVideos(outputAsJson.display);
      appendDisplay(workoutContainer);
    } else if (outputAsJson.intent.includes("memory.keyword")) {
      memoryContainer = createKeywordContainer(outputAsJson.display);
      appendDisplay(memoryContainer);
      addDisplayListeners(memoryContainer);
    } else if (outputAsJson.intent.includes("memory.time")) {
      memoryTimeContainer = makeConversationDiv(outputAsJson.display);
      appendDisplay(memoryTimeContainer);
    }
  }
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

function placeBooksUserInput(text, container, queryID) {
  if (container == "convo-container") {
    streamingContainer.innerHTML = "";
    streamingContainer.style.display = "none";
  }
  if (text != " (null) "){
    var formattedInput = text.substring(0, 1).toUpperCase() + text.substring(1); 
    placeObjectContainer("<p>" + formattedInput + "</p>", "user-side-" + queryID, container);
  }
}

function placeBooksFulfillment(text, queryID) {
  placeObjectContainer("<p>" + text + "</p>", "assistant-side-" + queryID, "convo-container");
  if (text.includes("Switching conversation language")) {
    window.sessionStorage.setItem("language", getLastWord(text));
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

/**
* Updates the frontend javascript to include the user's name (or nickname) in the title.
*
* @param name The name used to refer to the user.
*/
function updateName(name) {
  var greetingContainer = document.getElementsByName("greeting")[0];
  name = " " + name;
  greetingContainer.innerHTML = "<h1>Hi" + name + ", what can I help you with?</h1>";
}
