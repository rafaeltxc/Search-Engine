package com.rtxct.crawler.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.rtxct.crawler.dto.PageDTO;
import com.rtxct.crawler.utils.Helper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.NONE)
@NoArgsConstructor
@Component
public class Bot {
	/** Class properties. */
	private static final Logger logger = Logger.getLogger(Bot.class.getName());

	private final Semaphore semaphore = new Semaphore(1);

	private ExecutorService executorService;

	private Integer breakpoint;

	private List<PageDTO> pages;

	private List<String> visitedUrls;

	private Queue<String> tempUrlQueue;

	private Queue<String> urlQueue;

	/** Class Dependencies. */
	private Helper helper = new Helper();

	private PageDTO page;

	/**
	 * Bot class constructor.
	 * 
	 * @param rootUrlList List of URLs as Strings.
	 */
	public Bot(List<String> rootUrlList) {
		this(rootUrlList, 1);
	}

	/**
	 * Bot class constructor.
	 * 
	 * @param rootUrlList List of URLs as Strings.
	 * @param breakpoint  Limit how deep in the URLs the program should go.
	 */
	public Bot(List<String> rootUrlList, int breakpoint) {
		this.breakpoint = breakpoint;
		this.urlQueue = new LinkedList<String>(rootUrlList);
		propertiesInitializer();
	}

	/**
	 * Initializes the class properties.
	 */
	private void propertiesInitializer() {
		this.visitedUrls = new ArrayList<String>();
		this.pages = new ArrayList<PageDTO>();
		this.tempUrlQueue = new LinkedList<String>();
	}

	/**
	 * Crawls within threads the given URL, finding
	 * all the links inside it until the breakpoint is reached. The numbers of
	 * threads will be the same as the system's number.
	 * 
	 * @return Pages objects in a json representation.
	 */
	public List<PageDTO> crawlAsync() {
		int systemCores = Runtime.getRuntime().availableProcessors();
		return crawlAsync(systemCores);
	}

	/**
	 * Crawls within threads the given URL, finding
	 * all the links inside it until the breakpoint is reached. The number of
	 * threads will be represented by the maxThreads parameter.
	 * 
	 * @param maxThreads The maximum number of threads.
	 * @return Pages objects in a json representation.
	 */
	public List<PageDTO> crawlAsync(Integer maxThreads) {
		this.executorService = Executors.newFixedThreadPool(maxThreads);
		this.tempUrlQueue.clear();

		scrapeLinksAsync();
		this.breakpoint--;

		if (this.tempUrlQueue.size() > 0) {
			urlQueue.addAll(tempUrlQueue);
			return crawlAsync(maxThreads);
		}

		return this.pages;
	}

	/**
	 * Crawls synchronously the given URL finding all the links inside until the
	 * breakpoint is reached.
	 * 
	 * @return Pages objects in a json representation.
	 */
	public List<PageDTO> crawlSync() {
		this.tempUrlQueue.clear();

		scrapeLinksSync();

		this.breakpoint--;

		if (tempUrlQueue.size() > 0) {
			this.urlQueue.addAll(tempUrlQueue);
			return crawlSync();
		}

		return this.pages;
	}

	/**
	 * Loop through the URLs list, scraping the needed data and setting it to the
	 * corresponding global property. If the breakpoint is bigger than zero, get all
	 * links inside the URLs, and also set it to the corresponding global
	 * property for the next iteration.
	 */
	private void scrapeLinksAsync() {
		while (!this.urlQueue.isEmpty()) {
			String url = this.urlQueue.poll();
			this.visitedUrls.add(url);

			if (!helper.checkPageAvailability(url)) {
				continue;
			}

			try {
				this.executorService.execute(() -> {
					try {
						Document doc = Jsoup.connect(url).get();
						String title = doc.title();
						Element descDoc = doc.select("meta[name=description]").first();

						String desc = (descDoc != null) ? descDoc.attr("content") : "";
						page = PageDTO.builder().title(title).desc(desc).url(url).build();
						this.pages.add(page);

						Queue<String> returnedUrls = getLinks(doc, url);
						if (returnedUrls != null) {
							try {
								this.semaphore.acquire();
								this.tempUrlQueue = helper.mergeQueues(this.urlQueue, returnedUrls);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								logger.log(Level.INFO, "Semaphore error", e);
							} finally {
								this.semaphore.release();
							}
						}
					} catch (IOException e) {
						logger.log(Level.INFO, "ScrapeLinksAsync method error", e);
					}
				});
			} catch (Exception e) {
				logger.log(Level.INFO, "Executor error", e);
			}
		}

		this.executorService.shutdown();

		try {
			this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.log(Level.INFO, "Awaiting threads error", e);
		}
	}

	/**
	 * Loop through the URLs list, scraping the needed data and setting it to the
	 * corresponding global property. If the breakpoint is bigger than zero, get all
	 * links inside the URLs, and also set it to the corresponding global
	 * property for the next iteration.
	 */
	private void scrapeLinksSync() {
		while (!this.urlQueue.isEmpty()) {
			String url = this.urlQueue.poll();
			this.visitedUrls.add(url);

			if (!helper.checkPageAvailability(url)) {
				continue;
			}

			try {
				Document doc = Jsoup.connect(url).get();
				String title = doc.title();
				Element descDoc = doc.select("meta[name=description]").first();

				String desc = (descDoc != null) ? descDoc.attr("content") : "";
				page = PageDTO.builder().title(title).desc(desc).url(url).build();
				this.pages.add(page);

				Queue<String> returnedUrls = getLinks(doc, url);
				if (returnedUrls != null) {
					this.tempUrlQueue = helper.mergeQueues(this.urlQueue, returnedUrls);
				}
			} catch (IOException e) {
				logger.log(Level.INFO, "ScrapeLinksSync method error", e);
			}
		}
	}

	/**
	 * If breakpoint is bigger than 0, gets all the links inside the given URLs.
	 * 
	 * @param doc Document to retrieve links from.
	 * @param url Base URL for validation.
	 * @return List of all the links founded.
	 */
	private Queue<String> getLinks(Document doc, String url) {
		if (this.breakpoint <= 0) {
			return null;
		}

		try {
			Elements links = doc.select("a");

			Queue<String> urls = new LinkedList<>();
			links.forEach(element -> {
				String href = element.attr("href");

				if (href.length() > 0) {
					if (href.charAt(0) == '/') {
						href = helper.formatUrl(url, href);
					} else if (helper.validateURL(href) && !visitedUrls.contains(href)) {
						urls.add(href);
					}
				}
			});

			return urls;
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.INFO, "GetLinks method error", e);
		}
		return null;
	}
}
