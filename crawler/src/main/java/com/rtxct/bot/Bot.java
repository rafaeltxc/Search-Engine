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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rtxct.model.Page;
import com.rtxct.utils.Helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Component
/**
 * Bot class with crawler logic and methods.
 */
public class Bot {
	public static void main(String[] args) {
		// Testing
		Bot test = new Bot(Arrays.asList("https://www.wikipedia.org/", "https://www.youtube.com/"), 5);
		System.out.println(test.crawlSync());
	}

	/** Class properties. */
	private final Semaphore semaphore = new Semaphore(1);

	private ExecutorService executorService;

	private int breakpoint;

	private List<String> pages;

	private List<String> visitedUrls;

	private Queue<String> tempUrlQueue;

	private Queue<String> urlQueue;

	/** Class Dependencies. */
	@Autowired
	private Helper helper = new Helper();

	@Autowired
	private Page page;

	/**
	 * Bot class constructor with.
	 * 
	 * @param rootUrl Single URL as a String.
	 */
	public Bot(String rootUrl) {
		this(rootUrl, 0);
	}

	/**
	 * Bot class constructor with.
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
		executorService = Executors.newFixedThreadPool(maxThreads);
		tempUrlQueue.clear();

		scrapeLinksAsync();
		breakpoint--;

		if (tempUrlQueue.size() > 0) {
			urlQueue.addAll(tempUrlQueue);
			return crawlAsync(maxThreads);
		}

		return pages;
	}

	/**
	 * Crawls synchronously the given URL finding all the links inside until the
	 * breakpoint is reached.
	 * 
	 * @return Pages objects in a json representation.
	 */
	public List<String> crawlSync() {
		tempUrlQueue.clear();

		scrapeLinksSync();

		breakpoint--;

		if (tempUrlQueue.size() > 0) {
			urlQueue.addAll(tempUrlQueue);
			return crawlSync();
		}

		return pages;
	}

	private void scrapeLinksAsync() {
		while (!urlQueue.isEmpty()) {
			String url = urlQueue.poll();
			visitedUrls.add(url);

			if (!helper.checkPageAvaliability(url)) {
				continue;
			}

			try {
				executorService.execute(() -> {
					System.out.println(Thread.activeCount());
					try {
						Document doc = Jsoup.connect(url).get();
						String title = doc.title();
						Element descDoc = doc.select("meta[name=description]").first();

						String desc = (descDoc != null) ? descDoc.attr("content") : "";
						page = new Page(title, desc, url);
						pages.add(page.toJson());

						Queue<String> returnedUrls = getBreakpoint(doc, url);
						if (returnedUrls != null) {
							try {
								semaphore.acquire();
								tempUrlQueue = helper.mergeQueues(urlQueue, returnedUrls);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							} finally {
								semaphore.release();
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

		executorService.shutdown();

		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void scrapeLinksSync() {
		while (!urlQueue.isEmpty()) {
			String url = urlQueue.poll();
			visitedUrls.add(url);

			if (!helper.checkPageAvaliability(url)) {
				continue;
			}

			try {
				Document doc = Jsoup.connect(url).get();
				String title = doc.title();
				Element descDoc = doc.select("meta[name=description]").first();

				String desc = (descDoc != null) ? descDoc.attr("content") : "";
				page = new Page(title, desc, url);
				pages.add(page.toJson());

				Queue<String> returnedUrls = getBreakpoint(doc, url);
				if (returnedUrls != null) {
					tempUrlQueue = helper.mergeQueues(urlQueue, returnedUrls);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * If breakpoint is bigger than 0, gets all the links inside the given URLs.
	 * 
	 * @param links All the URLs to be accessed.
	 * @param url   Base URL for validation.
	 * @return List of all the links founded.
	 */
	private Queue<String> getBreakpoint(Document doc, String url) {
		if (breakpoint == 0) {
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
