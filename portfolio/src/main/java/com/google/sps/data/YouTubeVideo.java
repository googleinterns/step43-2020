package com.google.sps.data;

import com.google.gson.Gson;
import java.io.Serializable;

/**
 * YouTubeVideo class for Workout agent that has channel name and id of channel that posted video on
 * YouTube and title, description, thumbnail, id of video
 */
public class YouTubeVideo implements Serializable {
  private String channelTitle;
  private String title;
  private String description;
  private String thumbnail;
  private String videoURL = "https://www.youtube.com/watch?v=";
  private String channelURL = "https://www.youtube.com/channel/";
  private int currentIndex;
  private int videosDisplayedPerPage;
  private int currentPage;
  private int totalPages;
  private static final long serialVersionUID = 5716459602340197781L;

  public YouTubeVideo(
      String channelTitle,
      String title,
      String description,
      String thumbnail,
      String videoId,
      String channelId,
      int currentIndex,
      int videosDisplayedPerPage,
      int currentPage,
      int totalPages) {
    this.channelTitle = channelTitle;
    this.title = title;
    this.description = description;
    this.thumbnail = thumbnail;
    this.currentIndex = currentIndex;
    this.videosDisplayedPerPage = videosDisplayedPerPage;
    this.currentPage = currentPage;
    this.totalPages = totalPages;
    videoURL += videoId;
    channelURL += channelId;
  }

  public String toGson() {
    return new Gson().toJson(this);
  }
}
