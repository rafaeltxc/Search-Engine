package com.rtxct.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.rtxct.model.Page;
import com.rtxct.utils.Helper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.NONE)
@Component
/**
 * Bot class with crawler logic and methods.
 */
public class Bot {
	/** Class properties. */
	private final Semaphore semaphore = new Semaphore(1);

	private ExecutorService executorService;

	private int breakpoint;

	private List<String> pages;

	private List<String> visitedUrls;

	private Queue<String> tempUrlQueue;

	private Queue<String> urlQueue;

	/** Class Dependencies. */
	private Helper helper = new Helper();

	private Page page;

	/**
	 * Bot class constructor.
	 * 
	 * @param rootUrl Single URL as a String.
	 */
	public Bot(String rootUrl) {
		this(rootUrl, 0);
	}

	/**
	 * Bot class constructor.
	 * 
	 * @param rootUrl    Single URL as a String.
	 * @param breakpoint Limit how deep in the URLs the program should go.
	 */
	public Bot(String rootUrl, int breakpoint) {
		this.breakpoint = breakpoint;
		this.urlQueue = new LinkedList<String>(Arrays.asList(rootUrl));
		propertiesInitializer();
	}

	/**
	 * Bot class constructor.
	 * 
	 * @param rootUrlList List of URLs as Strings.
	 */
	public Bot(List<String> rootUrlList) {
		this(rootUrlList, 0);
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
		this.pages = new ArrayList<String>();
		this.tempUrlQueue = new LinkedList<String>();
	}

	/**
	 * Crawls within threads the given URL, finding
	 * all the links inside it until the breakpoint is reached. The numbers of
	 * threads will be the same as the system's number.
	 * 
	 * @return Pages objects in a json representation.
	 */
	public List<String> crawlAsync() {
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
	public List<String> crawlAsync(int maxThreads) {

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
	public List<String> crawlSync() {

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
						page = new Page(title, desc, url);
						this.pages.add(page.toJson());

						Queue<String> returnedUrls = getLinks(doc, url);
						if (returnedUrls != null) {
							try {
								this.semaphore.acquire();
								this.tempUrlQueue = helper.mergeQueues(this.urlQueue, returnedUrls);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							} finally {
								this.semaphore.release();
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		this.executorService.shutdown();

		try {
			this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
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
				page = new Page(title, desc, url);
				this.pages.add(page.toJson());

				Queue<String> returnedUrls = getLinks(doc, url);
				if (returnedUrls != null) {
					this.tempUrlQueue = helper.mergeQueues(this.urlQueue, returnedUrls);
				}
			} catch (IOException e) {
				e.printStackTrace();
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
		if (this.breakpoint == 0) {
			return null;
		}

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
	}
}
