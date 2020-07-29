var userId;
var userName;
var userEmail;
var savedWorkoutPlans;

/** Creates Workout Dashboard button on assistant main page that links to dashboard page*/
function createWorkoutDashboardButton(){
    var dashboardDiv = document.getElementsByClassName("workout-dashboard-link")[0];
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
    var name = document.createElement("h3");
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

    savedWorkoutPlansDiv = document.getElementsByClassName("saved-workouts")[0];
    savedWorkoutPlansTitle = document.createElement("h2");
    savedWorkoutPlansTitle.innerHTML = "Saved Workout Plans";
    savedWorkoutPlansDiv.appendChild(savedWorkoutPlansTitle);

    for (var i = 0; i < savedWorkoutPlans.length; i++) {
        createSavedWorkoutPlanCard(JSON.stringify(savedWorkoutPlans[i]));
    } 
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

    //Workout Plan Header
    var workoutPlanHeader = document.getElementsByClassName("workout-plan-header")[0];
    var workoutPlanTitle = document.createElement("h1");
    workoutPlanTitle.innerHTML = storedWorkoutPlan.workoutPlanName;
    workoutPlanHeader.appendChild(workoutPlanTitle);

    //Workout Progress
    var workoutPlanProgress = document.getElementsByClassName("workout-plan-progress")[0];
    var progress = document.createElement("h3");
    progress.id = "progress";
    var progressPercentage = storedWorkoutPlan.numWorkoutDaysCompleted / storedWorkoutPlan.planLength;
    progress.innerHTML = "Progress: " + progressPercentage;
    workoutPlanProgress.appendChild(progress);

    //Workout Plan Table
    dashboardWorkoutPlan = document.getElementsByClassName("workout-plan")[0];
    workoutPlanTable = createWorkoutPlanTable(storedWorkoutPlan, true, 1, false);
    dashboardWorkoutPlan.appendChild(workoutPlanTable);
}

/** Stores workoutPlanId to be able to display correct workout plan when link clicked
 *
 * @param workoutPlanId workoutPlanId to store correct workout plan
 */
function storeWorkoutPlanId(workoutPlanId) {
    window.localStorage.setItem("workout-plan-id", workoutPlanId);
}

