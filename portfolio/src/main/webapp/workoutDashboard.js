var userId;
var userName;
var userEmail;
var savedWorkoutPlans;
var savedWorkoutVideos;
var isUserLoggedIn = function() {getUserLoggedInStatus();};

/** Creates Workout Dashboard button on assistant main page that links to dashboard page*/
function createWorkoutDashboardButton(){
    dashboardDiv = document.getElementsByClassName("workout-dashboard-link")[0];
    var dashboardLink = document.createElement("a");
    dashboardLink.title = "Workout Dashboard";
    dashboardLink.innerHTML = "Workout Dashboard"
    dashboardLink.href = "dashboard.html";
    dashboardLink.target= "_blank";
    dashboardDiv.appendChild(dashboardLink);
}

/** Call all functions to get data from backend and display it*/
function dashboardSetup() {
    getUserInfo();
    getSavedWorkoutPlans();
    getSavedWorkoutVideos();
}

/** Get user info from servlet and call displayUserInfo()*/
function getUserInfo() {
    fetch('/workout-user-profile').then(response => response.text()).then((info) => {
        var infoJson = JSON.parse(info);
        userId = infoJson.userId;
        userName = infoJson.userName;
        userEmail = infoJson.userEmail;

        displayUserInfo();
    });
}


/** Displays user info on top of dashboard */
function displayUserInfo() {

    userInfoDiv = document.getElementsByClassName("user-info")[0];    

    //User profile picture
    var userPic = document.createElement("img");
    userPic.className = "user-pic";
    userPic.src = "https://icon-library.com/images/default-user-icon/default-user-icon-4.jpg";
    userInfoDiv.appendChild(userPic);

    //User contact info
    var userContact = document.createElement("div");
    userContact.className = "user-contact";
    var name = document.createElement("h1");
    name.innerHTML = userName;
    userContact.appendChild(name);
    var email = document.createElement("p");
    email.innerHTML = userEmail;
    userContact.appendChild(email);
    userInfoDiv.appendChild(userContact);
}

/** Gets user's saved workout plans from servlet and call displaySavedWorkoutPlans()*/
function getSavedWorkoutPlans() {
    fetch('/save-workouts').then(response => response.text()).then((workoutPlans) => {
        savedWorkoutPlans = JSON.parse(workoutPlans);
        displaySavedWorkoutPlans();
    });
}

/** Displays user's saved workout plans in dashboard */
function displaySavedWorkoutPlans() {

    savedWorkoutContent = document.getElementsByClassName("saved-workout-content")[0];
    savedWorkoutPlansDiv = document.getElementsByClassName("saved-workout-plans")[0];
    savedWorkoutPlansTitle = document.createElement("h2");
    savedWorkoutPlansTitle.innerHTML = "Saved Workout Plans";
    savedWorkoutPlansDiv.appendChild(savedWorkoutPlansTitle);

    for (var i = 0; i < savedWorkoutPlans.length; i++) {
        createSavedWorkoutPlanCard(JSON.stringify(savedWorkoutPlans[i]));
    } 

    savedWorkoutContent.appendChild(savedWorkoutPlansDiv);
}

/** Creates workout plan card to display on dashboard for each saved workout plan
 *
 * @param savedWorkoutPlan workout plan json object to create "card"-like displays
 */
function createSavedWorkoutPlanCard(savedWorkoutPlan) {
    workoutPlan = JSON.parse(savedWorkoutPlan);
    var workoutPlanId = workoutPlan.workoutPlanId.toString();

    //Storing workout plan to access in new workout plan tab
    var storageKey = "workout-plan-" + workoutPlanId;
    window.localStorage.setItem(storageKey, JSON.stringify(workoutPlan));

    //Getting list of videos
    workoutPlanVideos = workoutPlan.workoutPlanPlaylist;
    
    //Creating saved workout cards display for dashboard
    workoutPlanCardLink = document.createElement("a");
    workoutPlanCardLink.id = workoutPlanId;
    workoutPlanCardLink.title = workoutPlan.workoutPlanName;
    workoutPlanCardLink.href = "workoutPlan.html";
    workoutPlanCardLink.onclick = function() {storeWorkoutPlanId(this.id);};
    workoutPlanCardLink.target = "_blank";
    savedWorkoutPlansDiv.appendChild(workoutPlanCardLink);

    workoutPlanCard = document.createElement("div");
    workoutPlanCard.className = "workout-plan-card";
    workoutPlanCardLink.appendChild(workoutPlanCard);

    workoutPlanLink = document.createElement("a");
    workoutPlanCardLink.id = workoutPlanId;
    workoutPlanLink.title = workoutPlan.workoutPlanName;
    workoutPlanLink.innerHTML = workoutPlan.workoutPlanName;
    workoutPlanLink.href = "workoutPlan.html";
    workoutPlanLink.onclick = function() {storeWorkoutPlanId(this.id);};
    workoutPlanLink.target = "_blank";
    workoutPlanCard.appendChild(workoutPlanLink);

    workoutPlanDateCreated = document.createElement("p");
    workoutPlanDateCreated.innerHTML = workoutPlan.dateCreated;
    workoutPlanCard.appendChild(workoutPlanDateCreated);

}

/** Gets correct workout plan from localStorage and displays workout plan table that user can interact with */
function displayWorkoutPlan() {

    var workoutPlanId = window.localStorage.getItem("workout-plan-id");
    var storageKey = "workout-plan-" + workoutPlanId;
    var storedWorkoutPlan = window.localStorage.getItem(storageKey);
    storedWorkoutPlan = JSON.parse(storedWorkoutPlan);

    //Workout Plan Container
    var workoutPlanContainer = document.getElementsByClassName("workout-plan-container")[0];

    //Workout Plan Header
    var workoutPlanHeader = document.getElementsByClassName("workout-plan-header")[0];
    var workoutPlanTitle = document.createElement("h1");
    workoutPlanTitle.innerHTML = storedWorkoutPlan.workoutPlanName;
    workoutPlanHeader.appendChild(workoutPlanTitle);
    workoutPlanContainer.appendChild(workoutPlanHeader);

    //Workout Progress
    var workoutPlanProgress = document.getElementsByClassName("workout-plan-progress")[0];
    var progress = document.createElement("h3");
    progress.id = "progress";
    var progressPercentage = Math.round((storedWorkoutPlan.numWorkoutDaysCompleted / storedWorkoutPlan.planLength) * 100);
    progress.innerHTML = "Progress: " + progressPercentage + "%";
    workoutPlanProgress.appendChild(progress);
    workoutPlanContainer.appendChild(workoutPlanProgress);

    //Workout Plan Table
    var workoutPlanTable = document.getElementsByClassName("workout-plan-table")[0];
    var dashboardWorkoutPlan = createWorkoutPlanTable(storedWorkoutPlan, true, 1, false);
    workoutPlanTable.appendChild(dashboardWorkoutPlan);
    workoutPlanContainer.appendChild(workoutPlanTable);

    //Back to Dashboard Button Link
    var dashboardDiv = document.getElementsByClassName("dashboard-link")[0];
    var dashboardLink = document.createElement("a");
    dashboardLink.className = "back-to-dashboard-link";
    dashboardLink.title = "Back to Dashboard";
    dashboardLink.innerHTML = "Back to Dashboard"
    dashboardLink.href = "dashboard.html";
    dashboardDiv.appendChild(dashboardLink);
    workoutPlanContainer.appendChild(dashboardDiv);
}

/** Gets user's saved workout videos from servlet and call displaySavedWorkoutVideos()*/
function getSavedWorkoutVideos() {
    fetch('/save-video').then(response => response.text()).then((workoutVideos) => {
        savedWorkoutVideos = JSON.parse(workoutVideos);
        displaySavedWorkoutVideos();
    });
}

/** Displays user's saved workout videos in dashboard */
function displaySavedWorkoutVideos() {

    savedWorkoutContent = document.getElementsByClassName("saved-workout-content")[0];
    savedWorkoutVideosDiv = document.getElementsByClassName("saved-workout-videos")[0];
    savedWorkoutVideosTitle = document.createElement("h2");
    savedWorkoutVideosTitle.innerHTML = "Saved Workout Videos";
    savedWorkoutVideosDiv.appendChild(savedWorkoutVideosTitle);

    for (var i = 0; i < savedWorkoutVideos.length; i++) {
        createVideoContainer(JSON.stringify(savedWorkoutVideos[i]));
    } 

    savedWorkoutContent.appendChild(savedWorkoutVideosDiv);
}

/** Creates workout plan card to display on dashboard for each saved workout plan
 *
 * @param savedWorkoutVideo workout video json object to create display with thumbnail, title, and channel
 */
function createVideoContainer(savedWorkoutVideo) {
    workoutVideo = JSON.parse(savedWorkoutVideo);

    videoURL = workoutVideo.videoURL.replace(/"/g, "");
    thumbnail = workoutVideo.thumbnail.replace(/"/g, "");
    videoTitle = workoutVideo.title.replace(/"/g, "");
    channelURL = workoutVideo.channelURL.replace(/"/g, "");
    channelName = workoutVideo.channelTitle.replace(/"/g, "");

    if (videoTitle.length > 30) {
        videoTitle = videoTitle.substring(0, 30) + "..."; 
    }

    var savedWorkoutVideo = document.createElement("div");
    savedWorkoutVideo.className = "saved-video";
    
    //Saved Workout Video Thumbnail
    var savedVideoThumbnail = document.createElement("div");
    savedVideoThumbnail.className = "saved-video-thumbnail";

    var videoLink = document.createElement("a");
    videoLink.title = videoTitle;
    videoLink.href = videoURL;
    videoLink.target = "_blank";    

    var thumbnailImage = document.createElement("img");
    thumbnailImage.src = thumbnail;
    thumbnailImage.setAttribute("width", "250");
    thumbnailImage.setAttribute("height", "140");
    videoLink.appendChild(thumbnailImage);

    savedVideoThumbnail.appendChild(videoLink);
    savedWorkoutVideo.appendChild(savedVideoThumbnail);

    //Saved Video Title
    var videoTitleLink = document.createElement("a");
    videoTitleLink.title = videoTitle;
    videoTitleLink.href = videoURL;
    videoTitleLink.target = "_blank"; 

    var savedVideoTitle = document.createElement("h4");
    savedVideoTitle.className = "saved-video-title";

    savedVideoTitle.innerHTML = videoTitle;
    videoTitleLink.appendChild(savedVideoTitle);
    savedWorkoutVideo.appendChild(videoTitleLink);

    //Saved Video Channel Name
    var channelLink = document.createElement("a");
    channelLink.title = workoutVideo.channelTitle;
    channelLink.href = channelURL;
    channelLink.target = "_blank"; 

    var channelTitle = document.createElement("p");
    channelTitle.classList.add("channel-title");
    channelTitle.classList.add("saved-video-channel-title");
    channelTitle.innerHTML = channelName;
    channelLink.appendChild(channelTitle)
    savedWorkoutVideo.appendChild(channelLink);

    savedWorkoutVideosDiv.appendChild(savedWorkoutVideo);
}

/** Stores workoutPlanId to be able to display correct workout plan when link clicked
 *
 * @param workoutPlanId workoutPlanId to store correct workout plan
 */
function storeWorkoutPlanId(workoutPlanId) {
    window.localStorage.setItem("workout-plan-id", workoutPlanId);
}

