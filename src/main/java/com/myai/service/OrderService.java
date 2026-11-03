package com.myai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myai.dto.ApiDtos.BookDto;
import com.myai.dto.ApiDtos.CheckoutRequest;
import com.myai.dto.ApiDtos.OrderDto;
import com.myai.dto.DtoMapper;
import com.myai.exception.ApiException;
import com.myai.model.Address;
import com.myai.model.Book;
import com.myai.model.CartItem;
import com.myai.model.Order;
import com.myai.model.OrderItem;
import com.myai.model.OrderStatus;
import com.myai.model.User;
import com.myai.repository.AddressRepository;
import com.myai.repository.BookRepository;
import com.myai.repository.CartItemRepository;
import com.myai.repository.OrderRepository;
import com.myai.repository.UserRepository;

@Service
@Transactional
public class OrderService {

    /** One gift point is earned per this many rupees spent. */
    private static final BigDecimal RUPEES_PER_POINT_EARNED = BigDecimal.valueOf(100);

    private final OrderRepository orders;
    private final CartItemRepository cart;
    private final AddressRepository addresses;
    private final BookRepository books;
    private final UserRepository users;
    private final RecommendationService recommendations;
    private final BigDecimal taxRate;

    public OrderService(OrderRepository orders, CartItemRepository cart, AddressRepository addresses,
            BookRepository books, UserRepository users, RecommendationService recommendations,
            @Value("${app.tax-rate}") BigDecimal taxRate) {
        this.orders = orders;
        this.cart = cart;
        this.addresses = addresses;
        this.books = books;
        this.users = users;
        this.recommendations = recommendations;
        this.taxRate = taxRate;
    }

    public OrderDto checkout(Long userId, CheckoutRequest req) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        Address address = addresses.findByIdAndUserId(req.addressId(), userId)
                .orElseThrow(() -> ApiException.badRequest("Select a valid delivery address"));
        List<CartItem> items = cart.findByUserIdOrderByIdAsc(userId);
        if (items.isEmpty()) {
            throw ApiException.badRequest("Cart is empty");
        }

        // Lock the book rows so concurrent checkouts cannot oversell stock.
        Map<Long, Book> locked = books
                .lockAllByIdIn(items.stream().map(i -> i.getBook().getId()).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Book::getId, Function.identity()));

        BigDecimal subtotal = BigDecimal.ZERO;
        int maxDays = 0;
        Order order = new Order(user, address, req.paymentMethod(), "PAY-" + UUID.randomUUID());
        for (CartItem ci : items) {
            Book book = locked.get(ci.getBook().getId());
            if (ci.getQuantity() > book.getStock()) {
                throw ApiException.conflict("Only " + book.getStock() + " copies of '" + book.getTitle() + "' in stock");
            }
            book.setStock(book.getStock() - ci.getQuantity());
            book.setCopiesSold(book.getCopiesSold() + ci.getQuantity());
            order.getItems().add(new OrderItem(order, book, ci.getQuantity()));
            subtotal = subtotal.add(book.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            maxDays = Math.max(maxDays, book.getDeliveryDays());
        }

        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = subtotal.add(tax);

        // Gift points: 1 point = Rs 1, capped at the payable amount (whole rupees) and the balance.
        long maxRedeemable = Math.min(user.getGiftPoints(), gross.setScale(0, RoundingMode.DOWN).longValueExact());
        if (req.giftPointsToRedeem() > maxRedeemable) {
            throw ApiException.badRequest("You can redeem at most " + maxRedeemable + " gift points on this order");
        }
        long redeemed = req.giftPointsToRedeem();
        BigDecimal discount = BigDecimal.valueOf(redeemed).setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal total = gross.subtract(discount);
        long earned = total.divideToIntegralValue(RUPEES_PER_POINT_EARNED).longValue();

        // The gateway is simulated: payment always succeeds, a real integration goes here.
        user.setGiftPoints(user.getGiftPoints() - redeemed + earned);
        order.setTotals(subtotal, tax, discount, total, redeemed, earned, LocalDate.now().plusDays(maxDays));

        Order saved = orders.save(order);
        cart.deleteByUserId(userId);
        return DtoMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> history(Long userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId).stream().map(DtoMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto get(Long userId, Long orderId) {
        return DtoMapper.toDto(owned(userId, orderId));
    }

    /** "Buy In Again": previously purchased books. */
    @Transactional(readOnly = true)
    public List<BookDto> buyAgain(Long userId) {
        return recommendations.buyAgain(userId).stream().map(DtoMapper::toDto).toList();
    }

    public OrderDto cancel(Long userId, Long orderId) {
        Order order = owned(userId, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw ApiException.conflict("Order is already cancelled");
        }
        if (Instant.now().isAfter(order.getCreatedAt().plus(DtoMapper.CANCEL_WINDOW))) {
            throw ApiException.conflict("Orders can only be cancelled within 48 hours of purchase");
        }

        Set<Long> ids = order.getItems().stream().map(i -> i.getBook().getId()).collect(Collectors.toSet());
        Map<Long, Book> locked = books.lockAllByIdIn(ids).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        for (OrderItem item : order.getItems().stream().sorted(Comparator.comparing(OrderItem::getId)).toList()) {
            Book book = locked.get(item.getBook().getId());
            book.setStock(book.getStock() + item.getQuantity());
            book.setCopiesSold(Math.max(0, book.getCopiesSold() - item.getQuantity()));
        }

        // Refund: give back redeemed points and take back the points the order earned.
        User user = order.getUser();
        user.setGiftPoints(Math.max(0, user.getGiftPoints() + order.getGiftPointsRedeemed() - order.getGiftPointsEarned()));
        order.cancel();
        return DtoMapper.toDto(order);
    }

    private Order owned(Long userId, Long orderId) {
        return orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order"));
    }
}
