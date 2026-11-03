package com.myai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myai.dto.ApiDtos.AddToCartRequest;
import com.myai.dto.ApiDtos.CartDto;
import com.myai.dto.ApiDtos.CartItemDto;
import com.myai.dto.DtoMapper;
import com.myai.exception.ApiException;
import com.myai.model.Book;
import com.myai.model.CartItem;
import com.myai.model.User;
import com.myai.repository.BookRepository;
import com.myai.repository.CartItemRepository;
import com.myai.repository.UserRepository;

@Service
@Transactional
public class CartService {

    static final int MAX_QTY_PER_ITEM = 20;

    private final CartItemRepository cart;
    private final BookRepository books;
    private final UserRepository users;
    private final BigDecimal taxRate;

    public CartService(CartItemRepository cart, BookRepository books, UserRepository users,
            @Value("${app.tax-rate}") BigDecimal taxRate) {
        this.cart = cart;
        this.books = books;
        this.users = users;
        this.taxRate = taxRate;
    }

    @Transactional(readOnly = true)
    public CartDto get(Long userId) {
        return toDto(cart.findByUserIdOrderByIdAsc(userId));
    }

    public CartDto add(Long userId, AddToCartRequest req) {
        Book book = books.findById(req.bookId()).orElseThrow(() -> ApiException.notFound("Book"));
        User user = users.getReferenceById(userId);
        CartItem item = cart.findByUserIdAndBookId(userId, book.getId())
                .orElseGet(() -> new CartItem(user, book, 0));
        int newQty = item.getQuantity() + req.quantity();
        validateQuantity(book, newQty);
        item.setQuantity(newQty);
        cart.save(item);
        return get(userId);
    }

    public CartDto setQuantity(Long userId, Long itemId, int quantity) {
        CartItem item = owned(userId, itemId);
        validateQuantity(item.getBook(), quantity);
        item.setQuantity(quantity);
        return get(userId);
    }

    public CartDto remove(Long userId, Long itemId) {
        cart.delete(owned(userId, itemId));
        cart.flush();
        return get(userId);
    }

    public void clear(Long userId) {
        cart.deleteByUserId(userId);
    }

    /** Looks the item up by id and enforces ownership; 404 (not 403) so ids cannot be probed. */
    private CartItem owned(Long userId, Long itemId) {
        return cart.findById(itemId).filter(i -> i.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Cart item"));
    }

    private static void validateQuantity(Book book, int qty) {
        if (qty > MAX_QTY_PER_ITEM) {
            throw ApiException.badRequest("Maximum " + MAX_QTY_PER_ITEM + " copies per book");
        }
        if (qty > book.getStock()) {
            throw ApiException.conflict("Only " + book.getStock() + " copies of '" + book.getTitle() + "' in stock");
        }
    }

    CartDto toDto(List<CartItem> items) {
        List<CartItemDto> lines = items.stream()
                .map(i -> new CartItemDto(i.getId(), DtoMapper.toDto(i.getBook()), i.getQuantity(),
                        i.getBook().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        BigDecimal subtotal = lines.stream().map(CartItemDto::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        int maxDays = items.stream().map(i -> i.getBook().getDeliveryDays()).max(Comparator.naturalOrder()).orElse(0);
        return new CartDto(lines, subtotal, tax, subtotal.add(tax), items.isEmpty() ? null : LocalDate.now().plusDays(maxDays));
    }
}
