package com.rtxct.bot;

import java.io.IOException;
import java.util.ArrayList;
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
		Bot test = new Bot("https://www.wikipedia.org/");
		System.out.println(test.crawl(0));
	}

	/** Class properties. */
	private Queue<String> urlQueue;
	Queue<String> tempUrlQueue;
	private List<String> visitedUrls;
	private List<String> pages;

	/** Dependencies. */
	@Autowired
	private Helper helper = new Helper();
	@Autowired
	private Page page;

	/**
	 * Bot class constructor.
	 * 
	 * @param rootUrl The root given URL as a string.
	 */
	public Bot(String rootUrl) {
		this.urlQueue = new LinkedList<String>();
		this.tempUrlQueue = new LinkedList<String>();
		this.visitedUrls = new ArrayList<String>();
		this.pages = new ArrayList<String>();

		urlQueue.add(rootUrl);
	}

	/**
	 * Crwals the given URL finding all the links inside until the breakpoint is
	 * reached.
	 * 
	 * @param breakpoint Integer >= 0 to limit the recursion.
	 * @return Pages objects in a json representation.
	 */
	public List<String> crawl(int breakpoint) {
		// CLear list before getting in the logic, in case of the list was not empty.
		tempUrlQueue.clear();

		// Go through all the links.
		while (!urlQueue.isEmpty()) {
			// Remove from the urlQueue list, visited URL.
			String url = urlQueue.poll();
			// Add url to the visited URLs list
			visitedUrls.add(url);

			// Check if the page is online and able to crawl, if not, skip it.
			if (!helper.checkPageAvaliability(url)) {
				continue;
			}

			try {
				// Get actual URL page and it's children.
				Document doc = Jsoup.connect(url).get();
				String title = doc.title();
				Element descDoc = doc.select("meta[name=description]").first();
				Elements links = doc.select("a");

				// Create a new page object and add to the list as a json.
				String desc = (descDoc != null) ? descDoc.attr("content") : "";
				page = new Page(title, desc, url);
				pages.add(page.toJson());

				// Get new URLs from the source URL.
				Queue<String> returnedUrls = getBreakpoint(breakpoint, links, url);
				if (returnedUrls != null) {
					tempUrlQueue = helper.mergeQueues(urlQueue, returnedUrls);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		breakpoint--;

		// If the list is valid, go through all the links in it.
		if (tempUrlQueue.size() > 0) {
			urlQueue.addAll(tempUrlQueue);
			return crawl(breakpoint);
		}

		return pages;
	}

	/**
	 * If breakpoint is bigger than 0, gets all the links inside the given URLs.
	 * 
	 * @param breakpoint Integer >0 to limit the recursion.
	 * @param links      All the URLs to be accessed.
	 * @param url        Base URL for validation.
	 * @return List of all the links founded.
	 */
	private Queue<String> getBreakpoint(int breakpoint, Elements links, String url) {
		// Check breakpoint value.
		if (breakpoint == 0) {
			return null;
		}

		// Gets all the links in each URLs inside the given list.
		Queue<String> urls = new LinkedList<>();
		links.forEach(element -> {
			String href = element.attr("href");

			// Validate link and add to the temporary list.
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
