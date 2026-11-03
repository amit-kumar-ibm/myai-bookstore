package com.myai.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.myai.model.BookFormat;
import com.myai.model.OrderStatus;
import com.myai.model.PaymentMethod;
import com.myai.model.PaymentStatus;
import com.myai.model.Section;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response payloads for the REST API. */
public final class ApiDtos {

    private ApiDtos() {
    }

    // ---- Auth ----

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 100) String name) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record UserDto(Long id, String email, String name, long giftPoints) {
    }

    public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserDto user) {
    }

    // ---- Catalogue ----

    public record CategoryDto(Long id, String name) {
    }

    public record BookDto(Long id, String title, String author, String publisher, String description,
            BigDecimal price, BookFormat format, String language, CategoryDto category, int stock,
            double rating, int copiesSold, Section section, LocalDate tentativeDeliveryDate) {
    }

    public record BookDetailDto(BookDto book, List<BookDto> relatedBooks) {
    }

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }

    public record HomeDto(List<BookDto> recommended, List<BookDto> bestsellers, List<BookDto> newLaunches) {
    }

    // ---- Cart ----

    public record AddToCartRequest(
            @NotNull Long bookId,
            @Min(1) @Max(20) int quantity) {
    }

    public record UpdateCartItemRequest(@Min(1) @Max(20) int quantity) {
    }

    public record CartItemDto(Long id, BookDto book, int quantity, BigDecimal lineTotal) {
    }

    public record CartDto(List<CartItemDto> items, BigDecimal subtotal, BigDecimal tax, BigDecimal total,
            LocalDate tentativeDeliveryDate) {
    }

    // ---- Addresses ----

    public record AddressRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6 digit PIN") String pin,
            @NotBlank @Size(max = 100) String country,
            @NotBlank @Pattern(regexp = "\\+?[0-9]{7,15}", message = "must be a valid phone number") String phone) {
    }

    public record AddressDto(Long id, String firstName, String lastName, String line1, String line2,
            String city, String state, String pin, String country, String phone) {
    }

    // ---- Orders / payment ----

    /**
     * Checkout request. Raw card details are deliberately not accepted: the payment gateway is
     * simulated, and a real integration must tokenise cards client-side (PCI-DSS).
     */
    public record CheckoutRequest(
            @NotNull Long addressId,
            @NotNull PaymentMethod paymentMethod,
            @Min(0) long giftPointsToRedeem) {
    }

    public record OrderItemDto(Long bookId, String title, BigDecimal unitPrice, int quantity,
            BigDecimal lineTotal) {
    }

    public record OrderDto(Long id, OrderStatus status, PaymentMethod paymentMethod,
            PaymentStatus paymentStatus, String paymentReference, List<OrderItemDto> items,
            AddressDto address, BigDecimal subtotal, BigDecimal tax, BigDecimal discount,
            BigDecimal total, long giftPointsRedeemed, long giftPointsEarned,
            LocalDate estimatedDelivery, Instant createdAt, Instant cancellableUntil) {
    }
}
