# Web Crawler
### Overview
Ongoing project being built concurrently with [webSocket-backend-module](https://github.com/rafaeltxc/webSocket-backend-module).

Java based Web Crawler program designed to navigate through the web and extract relevant data from various websites. It is built to be scalable, allowing it to handle diverse websites and provide a robust solution for data aggregation.

### Features
 - **Scalable Web Crawling**: The crawler is able to navigate through the web, extracting data from various websites, and providing a reliable mechanism for data aggregation.
 
 - **Synchronous Operation**: The crawler operates synchronously, ensuring precise and ordered execution of tasks. Being beneficial for scenarios where sequential processing is crucial.
 
 - **Asynchronous Operations**: For increased efficiency and faster data gathering, the crawler supports asynchronous operations, using concurrent processes to enhance performance.
 
 - **API as a Service**: The project includes an API that allows users to use the web crawling capabilities as a service, making it convenient for integration into other applications.

### Usage

After cloning the respository, navigate to the project directory and initialize the project (Make sure you have Maven installed):
```console
mvn spring-boot:run
```

This will build and run the application using Maven. Alternatively, you can use your preferred tool to build and run the application.

To access the API documentation, in the container hosted port, open the app home page in your browser:
```console
http://localhost:8080/
```

### Docker

A docker image is also available in: [DockerHub](https://hub.docker.com/repository/docker/rtxct/crawler)

### How it works

The crawling process operates on both single URLs and lists of URLs. efore execution, users can specify the operation breakpoint. In asynchronous crawling, the program allows users to indicate the number of CPU cores for concurrent processing.


**Operation Breakpoints**:

The breakpoint determines the depth of the crawling process. A breakpoint of 1 (default) crawls and scrapes only the given page(s). A breakpoint greater than one extends the crawling to all links found on the given pages recursively, stopping when the breakpoint is reached.

**Note**: A breakpoint of 0 returns information only for the given page without scraping its URLs.


**Asynchronous Operations**:

In asynchronous mode, the program will create threads for concurrent execution, establishing an execution queue when the maximum number of active threads is reached. Each thread processes a URL, scrapes its data, and collects present links. Users can specify the number of CPU cores, and if not specified, the program utilizes all available cores.

**Note**: Specifying more cores than your system has won't make it any faster, as each thread occupies one core.
