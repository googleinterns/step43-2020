package com.google.sps.data;

import com.google.gson.Gson;

/**
 * YouTubeVideo class for Workout agent that has channel name and id of channel that posted video on
 * YouTube and title, description, thumbnail, id of video
 */
public final class YouTubeVideo {
  private String channelTitle;
  private String title;
  private String description;
  private String thumbnail;
  private String videoURL = "https://www.youtube.com/watch?v=";
  private String channelURL = "https://www.youtube.com/channel/";

  public YouTubeVideo(
      String channelTitle, String title, String description, String thumbnail, String videoId,  String channelId) {
    this.channelTitle = channelTitle;
    this.title = title;
    this.description = description;
    this.thumbnail = thumbnail;
    videoURL += videoId;
    channelURL += channelId;
  }

  public String toString() {
    return new Gson().toJson(this);
  }
}
