package com.myai.dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

import com.myai.dto.ApiDtos.AddressDto;
import com.myai.dto.ApiDtos.BookDto;
import com.myai.dto.ApiDtos.CategoryDto;
import com.myai.dto.ApiDtos.OrderDto;
import com.myai.dto.ApiDtos.OrderItemDto;
import com.myai.dto.ApiDtos.UserDto;
import com.myai.model.Address;
import com.myai.model.Book;
import com.myai.model.Category;
import com.myai.model.Order;
import com.myai.model.OrderItem;
import com.myai.model.User;

public final class DtoMapper {

    /** Orders may be cancelled within this window after being placed. */
    public static final Duration CANCEL_WINDOW = Duration.ofHours(48);

    private DtoMapper() {
    }

    public static UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getName(), u.getGiftPoints());
    }

    public static CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName());
    }

    public static BookDto toDto(Book b) {
        return new BookDto(b.getId(), b.getTitle(), b.getAuthor(), b.getPublisher(), b.getDescription(),
                b.getPrice(), b.getFormat(), b.getLanguage(), toDto(b.getCategory()), b.getStock(),
                b.getRating(), b.getCopiesSold(), b.getSection(),
                LocalDate.now().plusDays(b.getDeliveryDays()));
    }

    public static AddressDto toDto(Address a) {
        return new AddressDto(a.getId(), a.getFirstName(), a.getLastName(), a.getLine1(), a.getLine2(),
                a.getCity(), a.getState(), a.getPin(), a.getCountry(), a.getPhone());
    }

    public static OrderItemDto toDto(OrderItem i) {
        return new OrderItemDto(i.getBook().getId(), i.getTitle(), i.getUnitPrice(), i.getQuantity(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
    }

    public static OrderDto toDto(Order o) {
        return new OrderDto(o.getId(), o.getStatus(), o.getPaymentMethod(), o.getPaymentStatus(),
                o.getPaymentReference(), o.getItems().stream().map(DtoMapper::toDto).toList(),
                toDto(o.getAddress()), o.getSubtotal(), o.getTax(), o.getDiscount(), o.getTotal(),
                o.getGiftPointsRedeemed(), o.getGiftPointsEarned(), o.getEstimatedDelivery(),
                o.getCreatedAt(), o.getCreatedAt().plus(CANCEL_WINDOW));
    }
}
