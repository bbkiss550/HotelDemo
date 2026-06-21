package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "t_payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_payment`")
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "`ID_room`", referencedColumnName = "`ID_room`")
    private Room room;
    @Column(name = "p_amount")
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(name = "p_payment_date")
    private LocalDate paymentDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "p_status")
    private PaymentStatus status = PaymentStatus.UNPAID;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
}
