package com.google.sps.data;

import com.google.appengine.api.datastore.Entity;
import com.google.gson.Gson;
import java.util.List;

public class ConversationOutput {
  private String keyword;
  List<Pair<Entity, List<Entity>>> conversationList;

  public ConversationOutput(String keyword, List<Pair<Entity, List<Entity>>> conversationList) {
    this.keyword = keyword;
    this.conversationList = conversationList;
  }

  public String toString() {
    return new Gson().toJson(this);
  }
}
