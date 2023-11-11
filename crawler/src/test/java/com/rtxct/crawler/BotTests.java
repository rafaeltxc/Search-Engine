package com.rtxct.crawler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.util.Assert;

import com.rtxct.bot.Bot;

@TestInstance(Lifecycle.PER_CLASS)
@TestComponent
public class BotTests {

  private Bot bot;

  private String url;

  private String reponseMockup;

  @BeforeAll
  void test() {
    url = "https://google.com/";
    bot = new Bot(url);
  }

  @Test
  void getLinks() {

  }

}
