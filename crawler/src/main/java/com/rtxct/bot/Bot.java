package com.rtxct.bot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import lombok.Data;

/** Bot class */
@Data
public class Bot {

	public static void main(String[] args) {
		Bot test = new Bot();
		System.out.println(test.crawl("https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal", 0));
	}

	private Queue<String> urlQueue;
	private List<String> visitedUrls;
	private List<String> splitedRootUrl;

	/** Class constructor */
	public Bot() {
		this.urlQueue = new LinkedList<String>();
		this.visitedUrls = new ArrayList<String>();
		this.splitedRootUrl = new ArrayList<String>();
	}

	/** Fetch all the url's in the urlQueue list */
	public Queue<String> crawl(String rootUrl, int breakpoint) {
		if (!checkPageAvaliability(rootUrl)) {
		}

		setSplitedRootUrl(Arrays.asList(rootUrl.split("/")));
		urlQueue.add(rootUrl);
		visitedUrls.add(rootUrl);

		while (!urlQueue.isEmpty()) {
			try {
				Document doc = Jsoup.connect(rootUrl).get();
				Elements links = doc.select("a");

				links.forEach(element -> {
					String href = element.attr("href");

					if (href.charAt(0) == '/') {
						href = formatUrl(rootUrl, href);
					} else if (!visitedUrls.contains(href) && validateURL(href)) {
						visitedUrls.add(href);
						urlQueue.add(href);
					}
				});
				urlQueue.remove(rootUrl);

				return urlQueue;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return urlQueue;
	}

	/** Get page availability on connection */
	private boolean checkPageAvaliability(String url) {
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

	private String formatUrl(String baseUrl, String relativeUrl) {
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

	private boolean validateURL(String url) {
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
}
