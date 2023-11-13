package com.rtxct.crawler;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.context.TestComponent;

import com.rtxct.utils.Helper;

@TestComponent
public class HelperTests {

  private Helper helper = new Helper();

  /**
   * Check if page is online.
   */
  @Test
  public void checkPageAvailability() {
    String url = "https://google.com/";

    Assert.assertTrue(helper.checkPageAvailability(url));
  }

  /**
   * Format a base URL and a path together.
   */
  @Test
  public void formatUrl() {
    String baseUrl = "http://localhost/";
    String partialUrl = "/test/";

    String fullUrl = "http://localhost/test/";

    Assert.assertEquals(fullUrl, helper.formatUrl(baseUrl, partialUrl));
  }

  /**
   * Validates if given URL is valid.
   */
  @Test
  public void validateUrl() {
    String url = "http://localhost:0000";

    Assert.assertTrue(helper.validateURL(url));
  }

  /**
   * Merge two queues in one.
   */
  @Test
  public void mergeQueues() {
    Queue<String> base = new LinkedList<>(Arrays.asList("foo", "bar"));
    Queue<String> toMerge = new LinkedList<>(Arrays.asList("boo", "waa"));

    Queue<String> mockupQueue = new LinkedList<>(Arrays.asList("foo", "bar", "boo", "waa"));

    Assert.assertEquals(mockupQueue, helper.mergeQueues(base, toMerge));
  }
}
