package com.myai.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 150)
    private String author;

    /** The "brand" in the catalogue journey. */
    @Column(nullable = false, length = 150)
    private String publisher;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookFormat format;

    @Column(nullable = false, length = 50)
    private String language;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private int stock;

    /** Days from order until delivery; drives the tentative delivery date. */
    @Column(nullable = false)
    private int deliveryDays;

    @Column(nullable = false)
    private double rating;

    @Column(nullable = false)
    private int copiesSold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Section section = Section.STANDARD;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Book() {
    }

    public Book(String title, String author, String publisher, String description, BigDecimal price,
            BookFormat format, String language, Category category, int stock, int deliveryDays,
            double rating, int copiesSold, Section section) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.description = description;
        this.price = price;
        this.format = format;
        this.language = language;
        this.category = category;
        this.stock = stock;
        this.deliveryDays = deliveryDays;
        this.rating = rating;
        this.copiesSold = copiesSold;
        this.section = section;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BookFormat getFormat() {
        return format;
    }

    public String getLanguage() {
        return language;
    }

    public Category getCategory() {
        return category;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getDeliveryDays() {
        return deliveryDays;
    }

    public double getRating() {
        return rating;
    }

    public int getCopiesSold() {
        return copiesSold;
    }

    public void setCopiesSold(int copiesSold) {
        this.copiesSold = copiesSold;
    }

    public Section getSection() {
        return section;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
