package com.rtxct.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import lombok.Data;

@Data
@Component
@SuppressWarnings({ "resource" })
public class NginxTestContainer {

  public static void main(String[] args) {
    new NginxTestContainer();

    // while (true) {
    // try {
    // Thread.sleep(1000);
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // break;
    // }
    // }
  }

  private NginxContainer<?> nginx;
  private ExecutorService executorService = Executors.newSingleThreadExecutor();
  private String tmpDirectory = String.format("%s/Search-Engine/crawler/src/main/resources/templates/",
      System.getProperty("user.dir"));

  public NginxTestContainer() {
    try {
      this.createServer();

      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        this.startContainer();
      });

      future.get();

      this.pagesConfig();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void startContainer() {
    this.nginx.start();
  }

  public void stopContainer() {
    if (nginx != null) {
      this.nginx.close();
      this.executorService.shutdown();
    }
  }

  public String getServerPort() {
    try {
      return nginx.getBaseUrl("http", 80).toString();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void createServer() {
    this.nginx = new NginxContainer<>("nginx:1.20")
        .withFileSystemBind(tmpDirectory, "/usr/share/nginx/html")
        .withExposedPorts(80)
        .waitingFor(Wait.forHttp("/")
            .forStatusCode(403))
        .withStartupTimeout(Duration.ofSeconds(10));
  }

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

      PrintStream printStreamLink = new PrintStream(new FileOutputStream(linkFile));
      printStreamLink.println(link);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

  }
}
