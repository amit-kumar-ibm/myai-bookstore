package com.myai.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.myai.model.Book;
import com.myai.model.OrderItem;
import com.myai.model.OrderStatus;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Distinct books the user has bought (non-cancelled orders), most recent first. */
    @Query("""
            select oi.book from OrderItem oi
            where oi.order.user.id = :userId and oi.order.status <> :cancelled
            group by oi.book
            order by max(oi.order.createdAt) desc
            """)
    List<Book> findPurchasedBooks(@Param("userId") Long userId, @Param("cancelled") OrderStatus cancelled);
}
