package com.myai.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.myai.model.Book;
import com.myai.model.BookFormat;
import com.myai.model.Category;
import com.myai.model.Section;
import com.myai.repository.BookRepository;
import com.myai.repository.CategoryRepository;

/** Seeds the categories and the twelve books shown in the wireframes. No user accounts are seeded. */
@Component
@ConditionalOnProperty(name = "app.seed-catalog", havingValue = "true")
public class CatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

    private static final String[] CATEGORIES = { "Romance", "Mystery", "Science Fiction", "Fantasy",
            "Historical", "Biography", "Self-help", "Memoir", "Travel", "Cooking", "Children's",
            "Young Adult", "Comics & Graphic Novels", "Poetry", "Drama", "Science", "Philosophy",
            "Religion", "Language Learning", "Non-fiction", "Fiction", "Thriller", "Horror" };

    private final BookRepository books;
    private final CategoryRepository categories;

    public CatalogSeeder(BookRepository books, CategoryRepository categories) {
        this.books = books;
        this.categories = categories;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (books.count() > 0) {
            return;
        }
        Map<String, Category> cat = new HashMap<>();
        for (String name : CATEGORIES) {
            cat.put(name, categories.findByNameIgnoreCase(name).orElseGet(() -> categories.save(new Category(name))));
        }

        // Recommended (Section.STANDARD, high rated)
        add(cat, "The Art of Focus", "Arjun Patel", "Practical guide to mastering focus & boosting productivity every day.", "399", BookFormat.PAPERBACK, "Non-fiction", 4.6, 320, Section.STANDARD, 4);
        add(cat, "The Art of Learning", "Raj Patel", "Master the mindset and methods for effective lifelong learning.", "259", BookFormat.PAPERBACK, "Non-fiction", 4.4, 280, Section.STANDARD, 4);
        add(cat, "The Path to Success", "James Wright", "A practical guide to achieving goals with clarity and confidence.", "359", BookFormat.PAPERBACK, "Non-fiction", 4.5, 410, Section.STANDARD, 4);
        // Bestsellers this month
        add(cat, "The Midnight Hour", "James Adams", "Haunting tale of a man's journey & the shadows of a forgotten past.", "299", BookFormat.PAPERBACK, "Fiction", 4.2, 960, Section.BESTSELLER, 4);
        add(cat, "Beneath the Stars", "Jessica Martin", "A heartwarming tale, where two souls discover who you need.", "499", BookFormat.HARDCOVER, "Fiction", 4.7, 870, Section.BESTSELLER, 5);
        add(cat, "The Final Frontier", "Laura Mitchell", "A mission to space secrets to change humanity forever.", "359", BookFormat.PAPERBACK, "Fiction", 4.3, 790, Section.BESTSELLER, 4);
        // New launches
        add(cat, "Joy of Minimalism", "Daniel Reed", "Declutter your life to uncover peace, clarity, and joy. In The Joy of Minimalism, Daniel Reed guides you through practical strategies to declutter your home, space, and schedule.", "149", BookFormat.PAPERBACK, "Non-fiction", 4.8, 145, Section.NEW_LAUNCH, 4);
        add(cat, "The Vanishing House", "Clara Nelson", "A chilling mystery unfolds within a house that disappears.", "99", BookFormat.EBOOK, "Fiction", 4.0, 60, Section.NEW_LAUNCH, 0);
        add(cat, "The Lost Kitten", "Emily Parker", "A heartwarming tale of courage, friendship, and feline adventure.", "339", BookFormat.HARDCOVER, "Fiction", 4.1, 35, Section.NEW_LAUNCH, 5);
        log.info("Seeded {} categories and {} books", categories.count(), books.count());
    }

    private void add(Map<String, Category> cat, String title, String author, String description, String price,
            BookFormat format, String genre, double rating, int sold, Section section, int deliveryDays) {
        Category primary = switch (title) {
            case "The Art of Focus", "The Art of Learning", "The Path to Success", "Joy of Minimalism" -> cat.get("Self-help");
            case "The Midnight Hour" -> cat.get("Horror");
            case "Beneath the Stars" -> cat.get("Romance");
            case "The Final Frontier" -> cat.get("Science Fiction");
            case "The Vanishing House" -> cat.get("Mystery");
            default -> cat.get("Children's");
        };
        String publisher = switch (title) {
            case "Joy of Minimalism" -> "ABC Publishers";
            case "The Art of Focus", "The Art of Learning", "The Path to Success" -> "Insight Press";
            case "The Midnight Hour", "The Vanishing House" -> "Nightfall Books";
            default -> "Horizon House";
        };
        boolean ebook = format == BookFormat.EBOOK;
        books.save(new Book(title, author, publisher, description, new BigDecimal(price), format, "English",
                primary, ebook ? 1000 : 50, deliveryDays, rating, sold, section));
    }
}
