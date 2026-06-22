package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "t_receipt")
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_receipt`")
    private Long id;

    @Column(name = "r_receipt_no", nullable = false, unique = true)
    private String receiptNo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "`ID_guest`", referencedColumnName = "`ID_guest`")
    private Guest guest;

    @OneToOne(optional = false)
    @JoinColumn(name = "`ID_payment`", referencedColumnName = "`ID_payment`")
    private Payment payment;

    @Column(name = "r_receipt_date")
    private LocalDate receiptDate;

    @Column(name = "r_total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate receiptDate) { this.receiptDate = receiptDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
