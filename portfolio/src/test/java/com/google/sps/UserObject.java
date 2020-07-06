package com.google.sps.agents;

public class UserObject {

  private final String userID;
  private final String email;

  UserObject(String userID, String email) {
    this.userID = userID;
    this.email = email;
  }

  String getUserId() {
    return userID;
  }

  String getEmail() {
    return email;
  }
}
