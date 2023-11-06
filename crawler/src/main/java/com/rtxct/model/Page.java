package com.rtxct.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Page class */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Page {

  private String title;
  private String desc;
  private String url;

}
