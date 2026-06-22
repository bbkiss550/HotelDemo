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

    @ManyToOne
    @JoinColumn(name = "`ID_guest`", referencedColumnName = "`ID_guest`")
    private Guest guest;

    @Column(name = "p_amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "p_payment_date")
    private LocalDate paymentDate;

    @Column(name = "p_payment_type")
    private String paymentType = "ค่าเข้าพัก";

    @Column(name = "p_payment_method")
    private String paymentMethod = "เงินสด";

    @Column(name = "p_remark", length = 1000)
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(name = "p_status")
    private PaymentStatus status = PaymentStatus.UNPAID;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
}
