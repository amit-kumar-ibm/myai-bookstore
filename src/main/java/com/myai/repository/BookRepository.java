package com.myai.repository;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.myai.model.Book;
import com.myai.model.Section;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    List<Book> findBySection(Section section, Pageable pageable);

    /** Row-locks the books so concurrent checkouts cannot oversell stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id in :ids order by b.id")
    List<Book> lockAllByIdIn(@Param("ids") Collection<Long> ids);

    @Query("select distinct b.publisher from Book b order by b.publisher")
    List<String> findAllPublishers();

    @Query("select distinct b.publisher from Book b where b.category.id = :categoryId order by b.publisher")
    List<String> findPublishersByCategory(@Param("categoryId") Long categoryId);

    /** Related products: same category or author, excluding the book itself. */
    @Query("""
            select b from Book b
            where b.id <> :bookId and (b.category.id = :categoryId or b.author = :author)
            order by b.copiesSold desc
            """)
    List<Book> findRelated(@Param("bookId") Long bookId, @Param("categoryId") Long categoryId,
            @Param("author") String author, Pageable pageable);

    /** Recommendations: books in the given categories that are not already excluded. */
    @Query("""
            select b from Book b
            where b.category.id in :categoryIds and b.id not in :excludedIds
            order by b.rating desc, b.copiesSold desc
            """)
    List<Book> findRecommended(@Param("categoryIds") Collection<Long> categoryIds,
            @Param("excludedIds") Collection<Long> excludedIds, Pageable pageable);
}
