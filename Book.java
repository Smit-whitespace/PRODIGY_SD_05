package com.example.scraper;

public class Book {
    private final String title;
    private final String price;
    private final double priceNumeric;
    private final String rating;
    private final String availability;
    private final String productUrl;
    private final String scrapedAt;

    public Book(String title, String price, double priceNumeric, String rating, String availability, String productUrl, String scrapedAt) {
        this.title = title;
        this.price = price;
        this.priceNumeric = priceNumeric;
        this.rating = rating;
        this.availability = availability;
        this.productUrl = productUrl;
        this.scrapedAt = scrapedAt;
    }

    // getters
    public String getTitle() { return title; }
    public String getPrice() { return price; }
    public double getPriceNumeric() { return priceNumeric; }
    public String getRating() { return rating; }
    public String getAvailability() { return availability; }
    public String getProductUrl() { return productUrl; }
    public String getScrapedAt() { return scrapedAt; }
}
