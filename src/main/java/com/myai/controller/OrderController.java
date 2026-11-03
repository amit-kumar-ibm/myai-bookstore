package com.myai.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.myai.config.CurrentUser;
import com.myai.dto.ApiDtos.BookDto;
import com.myai.dto.ApiDtos.CheckoutRequest;
import com.myai.dto.ApiDtos.OrderDto;
import com.myai.dto.ApiDtos.UserDto;
import com.myai.dto.DtoMapper;
import com.myai.service.AuthService;
import com.myai.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orders;
    private final AuthService auth;

    public OrderController(OrderService orders, AuthService auth) {
        this.orders = orders;
        this.auth = auth;
    }

    /** Current user profile, including the gift-point balance shown at checkout. */
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return DtoMapper.toDto(auth.requireUser(CurrentUser.id(jwt)));
    }

    /** Payment and purchase: validates stock, redeems gift points, simulates payment, clears the cart. */
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest req) {
        return orders.checkout(CurrentUser.id(jwt), req);
    }

    @GetMapping("/orders")
    public List<OrderDto> history(@AuthenticationPrincipal Jwt jwt) {
        return orders.history(CurrentUser.id(jwt));
    }

    @GetMapping("/orders/buy-again")
    public List<BookDto> buyAgain(@AuthenticationPrincipal Jwt jwt) {
        return orders.buyAgain(CurrentUser.id(jwt));
    }

    @GetMapping("/orders/{id}")
    public OrderDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orders.get(CurrentUser.id(jwt), id);
    }

    /** Cancel within 48 hours of purchase. */
    @PostMapping("/orders/{id}/cancel")
    public OrderDto cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orders.cancel(CurrentUser.id(jwt), id);
    }
}
