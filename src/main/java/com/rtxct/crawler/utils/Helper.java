package com.rtxct.crawler.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class Helper {
  /**
   * Connects to the given URL and returns a boolean indicating whether the
   * connection was successful or not.
   * 
   * @param url String URL to connect to.
   * @return boolean indicating whether the connection was successful or not.
   */
  public boolean checkPageAvailability(String url) {
    try {
      Connection.Response page = Jsoup.connect(url).timeout(5000).execute();

      if (page.statusCode() != 200) {
        return false;
      }
      return true;

    } catch (IOException e) {
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Concatenates 2 separated URLs from the same site, into a single one.
   * 
   * @param baseUrl     Base URL of the site
   * @param relativeUrl Relative URL of the site
   * @return URL as a String
   */
  public String formatUrl(String baseUrl, String relativeUrl) {
    try {
      URL base = new URL(baseUrl);
      URL absoluteUrl = new URL(base, relativeUrl);

      return absoluteUrl.toString();
    } catch (MalformedURLException e) {
      return relativeUrl;
    } catch (Exception e) {
      return relativeUrl;
    }
  }

  /**
   * Validates the receiving URL through a Regex pattern, to check if it is in the
   * correct format.
   * 
   * @param url URL as a String to be validated.
   * @return Boolean whether the URL is in the correct format or not.
   */
  public boolean validateURL(String url) {
    try {
      String rgx = "\\b(https|http):\\/\\/+[^\\s]+[\\\\w]*";
      Pattern pattern = Pattern.compile(rgx);
      Matcher match = pattern.matcher(url);

      if (match.matches()) {
        return true;
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Merge 2 different queues into a single one.
   * 
   * @param base  Base queue to merge.
   * @param added Queue to merge.
   * @return New combined queue.
   */
  public Queue<String> mergeQueues(Queue<String> base, Queue<String> added) {
    try {
      Queue<String> mergedList = new LinkedList<>(base);
      mergedList.addAll(added);
      return mergedList;
    } catch (Exception e) {
      return null;
    }
  }
}
