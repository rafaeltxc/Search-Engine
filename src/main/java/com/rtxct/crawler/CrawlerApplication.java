package com.rtxct.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class CrawlerApplication {

  public static void main(String[] args) {
    SpringApplication.run(CrawlerApplication.class, args);
  }
}
