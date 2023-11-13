package com.rtxct.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import lombok.Data;

@Data
@Component
public class NginxTestContainer {

  /** Class properties */
  private NginxContainer<?> nginx;

  private ExecutorService executorService = Executors.newSingleThreadExecutor();

  private CountDownLatch countDown = new CountDownLatch(1);

  private String tmpDirectory = String.format("%s/src/main/resources/templates/",
      System.getProperty("user.dir"));

  /**
   * NginxTestContainer class constructor.
   * 
   * Creates the server configuration, start it, and after the container is up,
   * configure the nginx html pages.
   */
  public NginxTestContainer() {
    try {
      executorService.execute(() -> {
        this.createServer();

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
          this.startContainer();
        });

        try {
          future.get();
        } catch (InterruptedException e) {
          e.printStackTrace();
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }

        this.pagesConfig();
        this.awaitTermination();
      });
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Start Nginx testContainer.
   */
  public void startContainer() {
    try {
      this.nginx.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Stop Nginx testContainer.
   */
  public void stopContainer() {
    if (nginx != null) {
      try {
        this.nginx.close();
        this.executorService.shutdown();
        this.countDown.countDown();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Get container hosted port number.
   * 
   * @return Port number as String.
   */
  public String getServerPort() {
    try {
      return nginx.getBaseUrl("http", 80).toString();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Holds the container up until it reaches the closing command.
   */
  public void awaitTermination() {
    try {
      countDown.await();
    } catch (Exception e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    }
  }

  /**
   * Create and configure new Nginx testContainer.
   */
  @SuppressWarnings({ "resource" })
  private void createServer() {
    this.nginx = new NginxContainer<>("nginx:1.20")
        .withFileSystemBind(tmpDirectory, "/usr/share/nginx/html")
        .withExposedPorts(80)
        .waitingFor(Wait.forHttp("/")
            .forStatusCodeMatching(response -> response == 200 || response == 403))
        .withStartupTimeout(Duration.ofSeconds(10));
  }

  /**
   * Configures temporary html files for Nginx testContainer usage.
   */
  private void pagesConfig() {
    try {
      File indexFile = new File(tmpDirectory, "index.html");
      indexFile.deleteOnExit();
      String index = """
          <!DOCTYPE html>
          <html lang="en">
          <head>
            <meta charset="UTF-8">
            <title>Index</title>
            <meta name="description" content="Index description">
          </head>
          <body>
            <a href="%s/link.html">Link</a>
          </body>
          </html>
          """;
      String fmtIndex = String.format(index, getServerPort());

      File linkFile = new File(tmpDirectory, "link.html");
      linkFile.deleteOnExit();
      String link = """
          <!DOCTYPE html>
          <html lang="en">
          <head>
            <meta charset="UTF-8">
            <title>Link</title>
            <meta name="description" content="Link description">
          </head>
          <body>
            <a href="/">Link</a>
          </body>
          </html>
          """;

      PrintStream printStreamIndex = new PrintStream(new FileOutputStream(indexFile));
      printStreamIndex.println(fmtIndex);
      printStreamIndex.close();

      PrintStream printStreamLink = new PrintStream(new FileOutputStream(linkFile));
      printStreamLink.println(link);
      printStreamLink.close();
    } catch (FileNotFoundException e) {
      this.stopContainer();
      e.printStackTrace();
    }

  }
}
