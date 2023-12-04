package com.rtxct.crawler.model;

import java.util.List;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class BotModel {
  private List<String> urls;
}
