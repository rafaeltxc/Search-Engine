package com.rtxct.model;

import com.google.gson.Gson;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Page class as the page model
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Page {

  private String title;
  private String desc;
  private String url;

  public String toJson() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

}
