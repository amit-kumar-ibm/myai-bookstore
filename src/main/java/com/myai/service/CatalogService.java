package com.myai.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myai.dto.ApiDtos.BookDetailDto;
import com.myai.dto.ApiDtos.BookDto;
import com.myai.dto.ApiDtos.CategoryDto;
import com.myai.dto.ApiDtos.HomeDto;
import com.myai.dto.ApiDtos.PageDto;
import com.myai.dto.DtoMapper;
import com.myai.exception.ApiException;
import com.myai.model.Book;
import com.myai.model.BookFormat;
import com.myai.model.Section;
import com.myai.repository.BookRepository;
import com.myai.repository.CategoryRepository;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int RELATED_LIMIT = 6;
    private static final int HOME_SECTION_LIMIT = 4;

    private final BookRepository books;
    private final CategoryRepository categories;
    private final RecommendationService recommendations;

    public CatalogService(BookRepository books, CategoryRepository categories,
            RecommendationService recommendations) {
        this.books = books;
        this.categories = categories;
        this.recommendations = recommendations;
    }

    public List<CategoryDto> categories() {
        return categories.findAll(Sort.by("name")).stream().map(DtoMapper::toDto).toList();
    }

    public List<String> publishers(Long categoryId) {
        return categoryId == null ? books.findAllPublishers() : books.findPublishersByCategory(categoryId);
    }

    /** Landing page: personalised recommendations (if logged in), bestsellers and new launches. */
    public HomeDto home(Long userIdOrNull) {
        Pageable top = PageRequest.of(0, HOME_SECTION_LIMIT, Sort.by(Sort.Direction.DESC, "copiesSold"));
        Pageable newest = PageRequest.of(0, HOME_SECTION_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<BookDto> recommended = recommendations.forUser(userIdOrNull, HOME_SECTION_LIMIT).stream()
                .map(DtoMapper::toDto).toList();
        return new HomeDto(recommended,
                books.findBySection(Section.BESTSELLER, top).stream().map(DtoMapper::toDto).toList(),
                books.findBySection(Section.NEW_LAUNCH, newest).stream().map(DtoMapper::toDto).toList());
    }

    public PageDto<BookDto> search(String q, Long categoryId, String publisher, String language,
            BookFormat format, BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw ApiException.badRequest("minPrice must not exceed maxPrice");
        }
        Specification<Book> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String like = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((r, qr, cb) -> cb.or(
                    cb.like(cb.lower(r.get("title")), like, '\\'),
                    cb.like(cb.lower(r.get("author")), like, '\\')));
        }
        if (categoryId != null) {
            spec = spec.and((r, qr, cb) -> cb.equal(r.get("category").get("id"), categoryId));
        }
        if (publisher != null && !publisher.isBlank()) {
            spec = spec.and((r, qr, cb) -> cb.equal(cb.lower(r.get("publisher")), publisher.toLowerCase(Locale.ROOT)));
        }
        if (language != null && !language.isBlank()) {
            spec = spec.and((r, qr, cb) -> cb.equal(cb.lower(r.get("language")), language.toLowerCase(Locale.ROOT)));
        }
        if (format != null) {
            spec = spec.and((r, qr, cb) -> cb.equal(r.get("format"), format));
        }
        if (minPrice != null) {
            spec = spec.and((r, qr, cb) -> cb.greaterThanOrEqualTo(r.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((r, qr, cb) -> cb.lessThanOrEqualTo(r.get("price"), maxPrice));
        }

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<Book> result = books.findAll(spec, PageRequest.of(Math.max(page, 0), safeSize, sortFor(sort)));
        return new PageDto<>(result.getContent().stream().map(DtoMapper::toDto).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public BookDetailDto detail(Long id) {
        Book book = books.findById(id).orElseThrow(() -> ApiException.notFound("Book"));
        List<BookDto> related = new ArrayList<>(books
                .findRelated(book.getId(), book.getCategory().getId(), book.getAuthor(),
                        PageRequest.of(0, RELATED_LIMIT))
                .stream().map(DtoMapper::toDto).toList());
        return new BookDetailDto(DtoMapper.toDto(book), related);
    }

    /** Allow-list of sortable fields; anything else falls back to relevance (best selling). */
    private static Sort sortFor(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "copiesSold");
        }
        return switch (sort) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            case "rating" -> Sort.by(Sort.Direction.DESC, "rating");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "copiesSold");
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
