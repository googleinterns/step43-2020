package com.google.sps.data;

/** Ouput shown on web app screen. */
public final class Output {

  private final String userInput;
  private final String fulfillmentText;
  private final byte[] byteStringToByteArray;
  private final String display;
  private final String redirect;
  private final String intent;

  // Constructor without display
  public Output(
      String userInput, String fulfillmentText, byte[] byteStringToByteArray, String intent) {
    this(userInput, fulfillmentText, byteStringToByteArray, null, null, intent);
  }

  // Constructor with display
  public Output(
      String userInput,
      String fulfillmentText,
      byte[] byteStringToByteArray,
      String display,
      String intent) {
    this(userInput, fulfillmentText, byteStringToByteArray, display, null, intent);
  }

  // Constructor with display and redirect
  public Output(
      String userInput,
      String fulfillmentText,
      byte[] byteStringToByteArray,
      String display,
      String redirect,
      String intent) {
    this.userInput = userInput;
    this.fulfillmentText = fulfillmentText;
    this.byteStringToByteArray = byteStringToByteArray;
    this.display = display;
    this.redirect = redirect;
    this.intent = intent;
  }

  public String getUserInput() {
    return this.userInput;
  }

  public String getFulfillmentText() {
    return this.fulfillmentText;
  }

  public byte[] getByteStringToByteArray() {
    return this.byteStringToByteArray;
  }

  public String getDisplay() {
    return this.display;
  }

  public String getRedirect() {
    return this.redirect;
  }

  public String getIntent() {
    return this.intent;
  }
}
