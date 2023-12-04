package com.rtxct.crawler;

import java.io.IOException;
import java.util.logging.LogManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/** CrawlerApplication class */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class CrawlerApplication {

  public static void main(String[] args) {
    try {
      LogManager.getLogManager().readConfiguration(CrawlerApplication.class.getResourceAsStream("/logging.properties"));
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    SpringApplication.run(CrawlerApplication.class, args);
  }
}
