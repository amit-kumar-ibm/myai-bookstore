package com.myai.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "address_id")
    private Address address;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PLACED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(nullable = false, length = 64)
    private String paymentReference;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    private long giftPointsRedeemed;

    private long giftPointsEarned;

    @Column(nullable = false)
    private LocalDate estimatedDelivery;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Order() {
    }

    public Order(User user, Address address, PaymentMethod paymentMethod, String paymentReference) {
        this.user = user;
        this.address = address;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.paymentStatus = PaymentStatus.PAID;
    }

    public void setTotals(BigDecimal subtotal, BigDecimal tax, BigDecimal discount, BigDecimal total,
            long pointsRedeemed, long pointsEarned, LocalDate estimatedDelivery) {
        this.subtotal = subtotal;
        this.tax = tax;
        this.discount = discount;
        this.total = total;
        this.giftPointsRedeemed = pointsRedeemed;
        this.giftPointsEarned = pointsEarned;
        this.estimatedDelivery = estimatedDelivery;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.paymentStatus = PaymentStatus.REFUNDED;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Address getAddress() {
        return address;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public long getGiftPointsRedeemed() {
        return giftPointsRedeemed;
    }

    public long getGiftPointsEarned() {
        return giftPointsEarned;
    }

    public LocalDate getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
