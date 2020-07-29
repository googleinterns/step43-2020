var isUserLoggedIn = false;
var indexStart = 0;
var indexEnd = 5;
var numTotalVideos = 25;
var workoutPlanDay = 1;
var workoutPlannerDiv;
var plannerDiv;
var plannerTable;
var isUserLoggedIn = function() {getUserLoggedInStatus();};

/** Creates workout videos div that gets passed into appendDisplay method
 *
 * @param videoQuery list of all videos retuned by YouTube Data API call for workout.find intent
 */
function workoutVideos(videoQuery) {
  videos = JSON.parse(videoQuery);
  return createVideoDivs(videos, indexStart, indexEnd);
}

/** Creates workout planner div that gets passed into appendDisplay method
 *
 * @param workoutPlanQuery list of all videos in workout playlist created from YouTube Data API call
 */
function workoutPlanner(workoutPlanQuery) {
  workoutPlan = JSON.parse(workoutPlanQuery);
  workoutPlanDay = 1;
  return createWorkoutPlanTable(workoutPlan, false, workoutPlanDay, true);
}

/**
* Creates video page divs and adds extra video pages if possible
*
* @param videos JSON object of all videos
* @param indexStart index of first video on page
* @param indexEnd index of last video on page + 1
*/
function createVideoDivs(videos, indexStart, indexEnd) {
  
  workoutDiv = document.createElement("div");
  workoutDiv.className = "media-display";

  videosDiv = document.createElement("div");
  videosDiv.id = "videos";
  workoutDiv.appendChild(videosDiv);

  //Creating previous button
  previousButton = document.createElement("BUTTON");
  previousButton.classList.add("workout-buttons");
  previousButton.classList.add("video-previous-button");
  var buttonText = document.createTextNode("Previous");
  previousButton.appendChild(buttonText); 

  //Creating next button 
  nextButton = document.createElement("BUTTON");
  nextButton.classList.add("workout-buttons");
  nextButton.classList.add("video-next-button");
  var buttonText = document.createTextNode("Next");
  nextButton.appendChild(buttonText);

  for (var i = indexStart; i < indexEnd; i++) {
    video = videos[i];

    //Get video parameters and get rid of extra double quotes
    title = video.title.replace(/"/g, "");
    description = video.description.replace(/"/g, "");
    videoURL = video.videoURL.replace(/"/g, "");
    thumbnail = video.thumbnail.replace(/"/g, "");
    channelURL = video.channelURL.replace(/"/g, "");
    channelName = video.channelTitle.replace(/"/g, "");

    if (title.length > 80) { 
        title = title.substring(0, 80) + "..."; 
    }
    
    if (description.length > 170) {
        description = description.substring(0, 170) + "...";
    }

    currentIndex = video.currentIndex;
    videosDisplayedPerPage = video.videosDisplayedPerPage;
    currentPage = video.currentPage;
    totalPages = video.totalPages;
    replaceUnicode();

    var videoContainer = document.createElement("div");
    videoContainer.className = "video-container";

    //Video Thumbnail
    var videoThumbnail = document.createElement("div");
    videoThumbnail.className = "video-thumbnail";

    var videoLink = document.createElement("a");
    videoLink.title = title;
    videoLink.href = videoURL;
    videoLink.target = "_blank";    

    var thumbnailImage = document.createElement("img");
    thumbnailImage.src = thumbnail;
    thumbnailImage.setAttribute("width", "320");
    thumbnailImage.setAttribute("height", "180");
    videoLink.appendChild(thumbnailImage);
    
    videoThumbnail.appendChild(videoLink);
    videoContainer.appendChild(videoThumbnail);

    //Video Information
    var videoInfo = document.createElement("div");
    videoInfo.className = "video-info";

    var videoTitleLink = document.createElement("a");
    videoTitleLink.title = title;
    videoTitleLink.href = videoURL;
    videoTitleLink.target = "_blank"; 

    var videoTitle = document.createElement("h3");
    videoTitle.className = "video-title";
    videoTitle.innerHTML = title;
    videoTitleLink.appendChild(videoTitle);
    videoInfo.appendChild(videoTitleLink);

    var channelLink = document.createElement("a");
    channelLink.title = channelName;
    channelLink.href = channelURL;
    channelLink.target = "_blank"; 

    var channelTitle = document.createElement("p");
    channelTitle.className = "channel-title";
    channelTitle.innerHTML = channelName;
    channelLink.appendChild(channelTitle)
    videoInfo.appendChild(channelLink);

    var videoDescription = document.createElement("p");
    videoDescription.className = "video-description";
    videoDescription.innerHTML = description;
    videoInfo.appendChild(videoDescription);
    
    //Save Video Button (only add if user is logged in) 
    if (isUserLoggedIn) {
      saveVideoButton = document.createElement("BUTTON");
      saveVideoButton.id = i.toString();
      saveVideoButton.classList.add("workout-buttons");
      saveVideoButton.classList.add("save-video-button");
      var buttonText = document.createTextNode("Save Video");
      saveVideoButton.onclick = function() {saveWorkoutVideo(videos, this.id);};
      saveVideoButton.appendChild(buttonText); 
      videoInfo.appendChild(saveVideoButton);
    }

    videoContainer.appendChild(videoInfo);
    videosDiv.appendChild(videoContainer);

    footerDisplay = ((currentIndex + 1) % videosDisplayedPerPage == 0);
    //Create footer with page numbers and buttons under correct video div
    if (footerDisplay) {
        footer = document.createElement("div");
        footer.className = "footer";
        videosDiv.appendChild(footer);
        
        //Add page numbers to footer
        var pageNumbers = document.createElement("p");
        pageNumbers.className = "video-page-number";
        pageNumbers.innerHTML = currentPage + "/" + totalPages;
        footer.appendChild(pageNumbers);

        //Display previous button if not on first page
        if (currentPage != 1) {
          footer.appendChild(previousButton);
          footer.getElementsByClassName("workout-buttons video-previous-button").item(0).onclick = function() {showNewVideosPage(-5)};
        }
        //Display next button if not on last page
        if (currentPage != totalPages) {
          footer.appendChild(nextButton);
          footer.getElementsByClassName("workout-buttons video-next-button").item(0).onclick = function() {showNewVideosPage(5)};
        }
    }
  }

  return workoutDiv;
}

/**
* Creates new video display pages depending on whether previous or next button clicked
*
* @param numShiftIndex shifts index to show correct set of 5 videos depending on previous or next button clicked
*/
function showNewVideosPage(numShiftIndex) {

  //Remove existing divs 
  var mediaDisplayDivs = document.getElementsByClassName('media-display');
  var mediaDiv;
  for (mediaDiv of mediaDisplayDivs) {
      while (mediaDiv.firstChild) {
        mediaDiv.removeChild(mediaDiv.firstChild);
      }
  }

  //Create new divs
  indexStart += numShiftIndex
  indexEnd += numShiftIndex
  let workoutDiv = createVideoDivs(videos, indexStart, indexEnd);
  appendDisplay(workoutDiv);
}

/**
* Creates workout planner div with a table with the workout plan
*
* @param workoutPlan JSON object of WorkoutPlan object
* @param onDashboard boolean to know if table is on dashboard or assistant main page
* @param workoutPlanDay makes sure that each workout plan display starts at day 1
* @param addFooter boolean to decide if table should have a footer
*/
function createWorkoutPlanTable(workoutPlan, onDashboard, workoutPlanDay, addFooter) {
  var userId = workoutPlan.userId;
  var workoutPlanId = workoutPlan.workoutPlanId;
  var localStorageKey = userId + "-" + workoutPlanId;
  videos = workoutPlan.workoutPlanPlaylist;

  //Creating correct div depending on if workout plan table needs to be created on main assistant display or dashboard
  if (onDashboard) {
    workoutPlannerDiv = document.createElement("div");
    workoutPlannerDiv.className = "dashboard-workout-plan";
  } else {
    workoutPlannerDiv = document.createElement("div");
    workoutPlannerDiv.className = "media-display";
  }

  plannerDiv = document.createElement("div");
  plannerDiv.id = "workout-planner";
//   var plannerDivHeight =  (videos.length * 135) + 45;
//   plannerDiv.style.height = plannerDivHeight.toString() + "px";
  workoutPlannerDiv.appendChild(plannerDiv);

  plannerTable = document.createElement("div");
  plannerTable.className = "planner-table";
  plannerDiv.appendChild(plannerTable); 

  //Initialize workout plan info in localStorage if not already stored
  if (onDashboard && !window.localStorage.getItem(localStorageKey)) {
      initializeWorkoutPlanProgress(workoutPlan, localStorageKey);
  }

  //Creates new rows for workout plan table
  for (var i = 0; i < videos.length; i++) {
    createNewPlanTable(videos[i], workoutPlan, onDashboard);
  }

  //Only workout plan footer with save workout plan button if user logged in or view workout plan on YT if user is not logged in
  if (addFooter) {
      createWorkoutPlanFooter(workoutPlan);
  }
  
  return workoutPlannerDiv;
}

/**
* Creates a new row in the workout planner table (display shows new row, this creates new table)
*
* @param videos JSON object of videos in chunks of 5 videos
* @param workoutPlan JSON object workout plan to get information about user and workout plan if logged in 
* @param onDashboard boolean to know if table is on dashboard or assistant main page
*/

function createNewPlanTable(videos, workoutPlan, onDashboard) {

  var plannerTableRow = document.createElement("table");
  plannerTableRow.className = "planner-heading-data";
  plannerTable.appendChild(plannerTableRow);

  var headingTableRow = document.createElement("tr");
  headingTableRow.className = "planner-table-row-heading";
  plannerTableRow.appendChild(headingTableRow);

  var dataTableRow = document.createElement("tr");
  dataTableRow.className = "planner-table-row-data";
  plannerTableRow.appendChild(dataTableRow);

  for (var i = 0; i < videos.length; i++) {
      video = videos[i];

      channelName = video.channelTitle.replace(/"/g, "");
      title = video.title.replace(/"/g, "");
      videoURL = video.videoURL.replace(/"/g, "");
      
      if (title.length > 43) {
          title = title.substring(0, 43) + "...";
      }

      replaceUnicode();

      //Table Headings: Day xx
      var tableHeading = document.createElement("th");
      tableHeading.innerHTML = "Day " + workoutPlanDay;
      headingTableRow.appendChild(tableHeading);

      //Table Data: Workout Video Link
      tableData = document.createElement("td");
      dataTableRow.appendChild(tableData);

      var tableVideoLink = document.createElement("a");
      tableVideoLink.className = "table-video-link";
      tableVideoLink.title = title;
      tableVideoLink.href = videoURL;
      tableVideoLink.target = "_blank";

      var tableVideoTitle = document.createElement("p");
      tableVideoTitle.className = "table-video-title";
      tableVideoTitle.innerHTML = title;
      tableVideoLink.appendChild(tableVideoTitle);
      tableData.appendChild(tableVideoLink);

      //Add Mark Complete or Completed buttons if workout plan table on dashboard
      if (onDashboard) {
        var userId = workoutPlan.userId;
        var workoutPlanId = workoutPlan.workoutPlanId;
        var localStorageKey = userId + "-" + workoutPlanId;
        
        markCompletedButton = document.createElement("BUTTON");
        markCompletedButton.id = "day-" + workoutPlanDay;
        markCompletedButton.classList.add("workout-buttons");
        markCompletedButton.classList.add("mark-completed-button");

        var buttonText = getButtonText(markCompletedButton.id, localStorageKey);
        markCompletedButton.appendChild(buttonText); 
        tableData.appendChild(markCompletedButton);

        markCompletedButton.onclick = function() {markWorkoutAsCompleted(this.id, workoutPlan, localStorageKey);};
      }

      workoutPlanDay += 1;

  }

}

/** Created a footer with buttons to save workout plan and 
 *
 * @param workoutPlan workoutPlan JSON object to know which workout plan to save if save button clicked
 */
function createWorkoutPlanFooter(workoutPlan) {
    
    var workoutPlanFooter = document.createElement("div");
    workoutPlanFooter.className = "workout-plan-footer";
    plannerDiv.appendChild(workoutPlanFooter);

    if (!isUserLoggedIn) {
        //View Playlist Button (if user not logged in) 
        viewPlaylistButton = document.createElement("BUTTON");
        viewPlaylistButton.classList.add("workout-plan-footer-buttons");
        viewPlaylistButton.classList.add("workout-buttons");
        var buttonText = document.createTextNode("View Playlist");
        viewPlaylistButton.appendChild(buttonText); 
        workoutPlanFooter.appendChild(viewPlaylistButton);

        var playlistURL = "https://www.youtube.com/playlist?list=" + workoutPlan.playlistId;
        viewPlaylistButton.onclick = function() {window.open(playlistURL, "_blank");};

    } else {
        //Save Workout Plan Button (if user logged in)
        saveWorkoutPlanButton = document.createElement("BUTTON");
        saveWorkoutPlanButton.classList.add("workout-plan-footer-buttons");
        saveWorkoutPlanButton.classList.add("workout-buttons");
        var buttonText = document.createTextNode("Save Workout Plan");
        saveWorkoutPlanButton.appendChild(buttonText); 
        workoutPlanFooter.appendChild(saveWorkoutPlanButton);

        saveWorkoutPlanButton.onclick = function() {saveWorkoutPlan(workoutPlan);};
    }
}

/** Marks day in workout plan as completed when button clicked and saves this is localStorage so user can access this information even when page is refreshed 
 *
 * @param buttonId buttonId to keep track of which workout day was marked as complete (depending on which button clicked)
 * @param workoutPlan workoutPlan JSON object to know which workout plan to update progress about
 * @param localStorageKey key for localStorage to access and update correct workout plan for correct user
 */
function markWorkoutAsCompleted(buttonId, workoutPlan, localStorageKey) {

    //Changing button text to show that workout plan day was completed
    var buttonToMark = document.getElementById(buttonId);
    var oldButtonText = buttonToMark.childNodes[0];

    //Nothing should happen if the user presses the button when it was already marked complete
    if (oldButtonText.textContent == "Mark Completed") {
        buttonToMark.removeChild(oldButtonText);
        var newButtonText = document.createTextNode("Completed!");
        buttonToMark.appendChild(newButtonText); 

        //Storing this button text so workout progress is accurate when page refreshed
        var workoutProgressInfoJson = JSON.parse(window.localStorage.getItem(localStorageKey));
        workoutProgressInfoJson["numWorkoutDaysCompleted"] += 1;
        var workoutProgressButtonTextJson = JSON.parse(workoutProgressInfoJson["workoutProgressButtonText"]);
        workoutProgressButtonTextJson[buttonId] = "Completed!";
        workoutProgressInfoJson["workoutProgressButtonText"] = JSON.stringify(workoutProgressButtonTextJson);
        var workoutProgressInfoString = JSON.stringify(workoutProgressInfoJson);
        window.localStorage.setItem(localStorageKey, workoutProgressInfoString);
        updateWorkoutPlanProgress(workoutPlan, localStorageKey);
    }
    
}

/** Initialized workout plan in localStorage to workout progress can be updated in the future
 *
 * @param workoutPlan workoutPlan JSON object to know which workout plan to initialize progress about
 * @param localStorageKey key for localStorage to initialize correct workout plan for correct user
 */
function initializeWorkoutPlanProgress(workoutPlan, localStorageKey) {

    var planLength = workoutPlan.planLength;
    var workoutProgressInfo = {};

    //Create JSON to store workout progress for each day
    var workoutPlanProgressJson = {};
    for (var i = 1; i <= planLength; i++) {
        var day = "day-"+ i.toString();
        workoutPlanProgressJson[day] = "Mark Completed";
    }
    var workoutPlanProgressString = JSON.stringify(workoutPlanProgressJson);

    //Initialize number of days worked out to 0 and initialize JSON to track completed workouts for each day
    workoutProgressInfo["numWorkoutDaysCompleted"] = 0;
    workoutProgressInfo["workoutProgressButtonText"] = workoutPlanProgressString;
    var workoutProgressInfoString = JSON.stringify(workoutProgressInfo);
    window.localStorage.setItem(localStorageKey, workoutProgressInfoString);
}

/** Gets button text for each button in workout plan table to show user if workout plan day is already completed or not
 *
 * @param buttonId buttonId to know which button's text needs to be returned
 * @param localStorageKey key for localStorage to find info for correct workout plan for correct user
 */
function getButtonText(buttonId, localStorageKey) {
    var workoutProgressInfoJson = JSON.parse(window.localStorage.getItem(localStorageKey));
    var workoutProgressButtonTextJson = JSON.parse(workoutProgressInfoJson["workoutProgressButtonText"]);
    var buttonText = workoutProgressButtonTextJson[buttonId]
    return document.createTextNode(buttonText);
}

/** Updates workout plan progress when "Mark Complete" button is clicked
 *
 * @param workoutPlan workoutPlan string with userId and workoutPlanId 
 * @param localStorageKey key for localStorage to update info for correct workout plan for correct user
 */

function updateWorkoutPlanProgress(workoutPlan, localStorageKey){

  //Create new JSON oject for workout plan to be saved
  var updatedWorkoutPlan = new Object();
  updatedWorkoutPlan.userId = workoutPlan.userId;
  updatedWorkoutPlan.workoutPlanId  = workoutPlan.workoutPlanId;
  var workoutPlanString= JSON.stringify(updatedWorkoutPlan);

  var numWorkoutDaysCompleted = JSON.parse(window.localStorage.getItem(localStorageKey))["numWorkoutDaysCompleted"];

  //Update workout plan progress display on dashboard
  var progress = document.getElementById("progress");
  var progressPercentage = Math.round((numWorkoutDaysCompleted / workoutPlan.planLength) * 100);
  progress.innerHTML = "Progress: " + progressPercentage + "%";

  //Update workout plan progress
  fetch('/workout-plan-progress' + '?workout-plan=' + workoutPlanString + '&num-workout-days-completed=' + numWorkoutDaysCompleted, {
      method: 'POST'
  }).then(response => response.text()).then(() => {
      console.log('Updated workout plan progress');
  });
}

/** Replaces unicode strings with actual characters */
function replaceUnicode() {

    //Properly format apostrophes
    channelName = channelName.replace("\\u0027", "'");
    title = title.replace("\\u0027", "'");
    description = description.replace("\\u0027", "'");    

    //Properly format ampersands
    channelName = channelName.replace("\\u0026", "&").replace("\\u0026amp;", "&");
    title = title.replace("\\u0026", "&").replace("\\u0026amp;", "&");
    description = description.replace("\\u0026", "&").replace("\\u0026amp;", "&");
}
