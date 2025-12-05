package com.example.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Scraper {

    private static final String DEFAULT_USER_AGENT = "JavaScraperGUI/1.0 (+https://example.com/contact)";

    /**
     * Scrapes pages starting from startUrl until no next page or until maxPages reached (if maxPages > 0).
     * This method is synchronous and can be called from a background thread.
     *
     * @param startUrl starting page (e.g., http://books.toscrape.com/)
     * @param maxPages max pages to fetch (<=0 means all)
     * @param minDelayMillis politeness delay lower bound (ms)
     * @param maxDelayMillis politeness delay upper bound (ms)
     * @param progressCallback optional callback to receive number of items scraped so far (may be null)
     * @return list of Book objects
     * @throws Exception on network/parse errors
     */
    public List<Book> scrape(String startUrl, int maxPages, int minDelayMillis, int maxDelayMillis, ProgressCallback progressCallback) throws Exception {
        List<Book> results = new ArrayList<>();
        String current = startUrl;
        int pageCount = 0;

        while (current != null) {
            pageCount++;
            if (maxPages > 0 && pageCount > maxPages) break;

            Document doc = Jsoup.connect(current)
                    .userAgent(DEFAULT_USER_AGENT)
                    .timeout(15000)
                    .get();

            Elements cards = doc.select("article.product_pod");
            for (Element c : cards) {
                String title = c.selectFirst("h3 > a").attr("title").trim();
                String rel = c.selectFirst("h3 > a").attr("href").trim();
                String productUrl = URI.create(current).resolve(rel).toString();
                String price = c.selectFirst(".price_color").text().trim();
                double priceNumeric = parsePrice(price);
                String availability = c.selectFirst(".availability").text().trim();
                String rating = parseRatingClass(c.selectFirst(".star-rating"));
                String scrapedAt = Instant.now().toString();

                results.add(new Book(title, price, priceNumeric, rating, availability, productUrl, scrapedAt));
                if (progressCallback != null) progressCallback.onProgress(results.size());
            }

            Element next = doc.selectFirst("li.next > a");
            if (next != null) {
                String nextHref = next.attr("href");
                current = URI.create(current).resolve(nextHref).toString();
            } else {
                current = null;
            }

            // politeness: randomized delay
            int sleepMs = ThreadLocalRandom.current().nextInt(minDelayMillis, maxDelayMillis + 1);
            Thread.sleep(sleepMs);
        }

        return results;
    }

    private static double parsePrice(String priceStr) {
        if (priceStr == null) return Double.NaN;
        String cleaned = priceStr.replaceAll("[^0-9.\\-]", "");
        try { return Double.parseDouble(cleaned); }
        catch (NumberFormatException e) { return Double.NaN; }
    }

    private static String parseRatingClass(Element starEl) {
        if (starEl == null) return "";
        for (String cls : starEl.classNames()) {
            switch (cls) {
                case "One": return "1";
                case "Two": return "2";
                case "Three": return "3";
                case "Four": return "4";
                case "Five": return "5";
            }
        }
        return "";
    }

    public interface ProgressCallback {
        void onProgress(int itemsScraped);
    }
}
