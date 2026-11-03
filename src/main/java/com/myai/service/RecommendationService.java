package com.myai.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myai.model.Book;
import com.myai.model.OrderStatus;
import com.myai.repository.BookRepository;
import com.myai.repository.OrderItemRepository;

/** "Recommends items based on Order History" and the "Buy In Again" list. */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final BookRepository books;
    private final OrderItemRepository orderItems;

    public RecommendationService(BookRepository books, OrderItemRepository orderItems) {
        this.books = books;
        this.orderItems = orderItems;
    }

    /** Books the user has bought before, most recent first. */
    public List<Book> buyAgain(Long userId) {
        return orderItems.findPurchasedBooks(userId, OrderStatus.CANCELLED);
    }

    /**
     * Top-rated books from categories the user has purchased from, excluding books already bought.
     * Guests and users without history get the top-rated books overall.
     */
    public List<Book> forUser(Long userIdOrNull, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        if (userIdOrNull != null) {
            List<Book> purchased = buyAgain(userIdOrNull);
            if (!purchased.isEmpty()) {
                Set<Long> categoryIds = purchased.stream().map(b -> b.getCategory().getId())
                        .collect(Collectors.toCollection(HashSet::new));
                Set<Long> excluded = purchased.stream().map(Book::getId).collect(Collectors.toSet());
                List<Book> recs = books.findRecommended(categoryIds, excluded, page);
                if (!recs.isEmpty()) {
                    return recs;
                }
            }
        }
        return books.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "rating", "copiesSold")))
                .getContent();
    }
}
