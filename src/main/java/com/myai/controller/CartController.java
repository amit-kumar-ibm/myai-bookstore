package com.myai.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myai.config.CurrentUser;
import com.myai.dto.ApiDtos.AddToCartRequest;
import com.myai.dto.ApiDtos.CartDto;
import com.myai.dto.ApiDtos.UpdateCartItemRequest;
import com.myai.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @GetMapping
    public CartDto get(@AuthenticationPrincipal Jwt jwt) {
        return cart.get(CurrentUser.id(jwt));
    }

    @PostMapping("/items")
    public CartDto add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddToCartRequest req) {
        return cart.add(CurrentUser.id(jwt), req);
    }

    @PatchMapping("/items/{itemId}")
    public CartDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest req) {
        return cart.setQuantity(CurrentUser.id(jwt), itemId, req.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    public CartDto remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        return cart.remove(CurrentUser.id(jwt), itemId);
    }
}
