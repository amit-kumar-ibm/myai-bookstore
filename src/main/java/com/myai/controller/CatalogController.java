package com.myai.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.myai.config.CurrentUser;
import com.myai.dto.ApiDtos.BookDetailDto;
import com.myai.dto.ApiDtos.BookDto;
import com.myai.dto.ApiDtos.CategoryDto;
import com.myai.dto.ApiDtos.HomeDto;
import com.myai.dto.ApiDtos.PageDto;
import com.myai.model.BookFormat;
import com.myai.service.CatalogService;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    /** Landing page. Public; personalised when a valid bearer token is supplied. */
    @GetMapping("/home")
    public HomeDto home(@AuthenticationPrincipal Jwt jwt) {
        return catalog.home(CurrentUser.idOrNull(jwt));
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return catalog.categories();
    }

    /** Brands (publishers), optionally limited to one category. */
    @GetMapping("/publishers")
    public List<String> publishers(@RequestParam(required = false) Long categoryId) {
        return catalog.publishers(categoryId);
    }

    @GetMapping("/books")
    public PageDto<BookDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) BookFormat format,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return catalog.search(q, categoryId, publisher, language, format, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/books/{id}")
    public BookDetailDto detail(@PathVariable Long id) {
        return catalog.detail(id);
    }
}
