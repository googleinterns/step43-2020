package com.google.sps.servlets;

public class AuthOutput {
  private String authText;
  private String displayName;
  private String logButton;

  AuthOutput(String authText, String displayName, String logButton) {
    this.authText = authText;
    this.displayName = displayName;
    this.logButton = logButton;
  }
}
