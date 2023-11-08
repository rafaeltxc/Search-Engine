package com.rtxct.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
@Builder
@Component
/**
 * Bot class with crawler logic and methods.
 */
public class Bot {
	public static void main(String[] args) {
		// Testing
		Bot test = new Bot("https://www.wikipedia.org/", 0);
		System.out.println(test.crawlSync());
	}

	/** Class properties. */
	@Builder.Default
	private int breakpoint = 0;

	private List<String> pages;

	private List<String> visitedUrls;

	private Queue<String> tempUrlQueue;

	private Queue<String> urlQueue;

	/** Dependencies. */
	@Autowired
	private Helper helper = new Helper();

	@Autowired
	private Page page;

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
	 * Bot class constructor with.
	 * 
	 * @param rootUrl Single URL as a String.
	 */
	public Bot(String rootUrl) {
		this.urlQueue = new LinkedList<String>(Arrays.asList(rootUrl));
		propertiesInitializer();
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
	 * Bot class constructor.
	 * 
	 * @param rootUrlList List of URLs as Strings.
	 */
	public Bot(List<String> rootUrlList) {
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
	 * Crawls within threads the given URL finding all the links inside until the
	 * breakpoint is
	 * reached.
	 * 
	 * @return Pages objects in a json representation.
	 */
	public List<String> crawlAsync() {
		tempUrlQueue.clear();

		scrapeLinksAsync();

		breakpoint--;

		if (tempUrlQueue.size() > 0) {
			urlQueue.addAll(tempUrlQueue);
			return crawlAsync();
		}

		return pages;
	}

	/**
	 * Crawls synchronously the given URL finding all the links inside until the
	 * breakpoint is
	 * reached.
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
				Elements links = doc.select("a");

				String desc = (descDoc != null) ? descDoc.attr("content") : "";
				page = new Page(title, desc, url);
				pages.add(page.toJson());

				Queue<String> returnedUrls = getBreakpoint(links, url);
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
	private Queue<String> getBreakpoint(Elements links, String url) {
		if (breakpoint == 0) {
			return null;
		}

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
