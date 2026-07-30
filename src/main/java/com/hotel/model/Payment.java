package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_payment`")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "`ID_room`", referencedColumnName = "`ID_room`")
    private Room room;

    @ManyToOne
    @JoinColumn(name = "`ID_guest`", referencedColumnName = "`ID_guest`")
    private Guest guest;

    @Column(name = "p_amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "p_fine_amount")
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(name = "p_payment_date")
    private LocalDate paymentDate;

    @Column(name = "p_payment_method")
    private String paymentMethod = "เงินสด";

    @OneToOne
    @JoinColumn(name = "`ID_reciept`", referencedColumnName = "`ID_reciept`")
    private Reciept reciept;

    @ManyToOne
    @JoinColumn(name = "ID_monthly_rent_bill", referencedColumnName = "ID_monthly_rent_bill")
    private MonthlyRentBill monthlyRentBill;

    @ManyToOne
    @JoinColumn(name = "`ID_booking`", referencedColumnName = "`ID_booking`")
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "`ID_deposit_refund`", referencedColumnName = "`ID_deposit_refund`")
    private DepositRefund depositRefund;

    @Column(name = "p_remark", length = 1000)
    private String remark;

    @Column(name = "id_payment_status", nullable = false)
    private Long statusId = 2L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_payment_status", insertable = false, updatable = false)
    private PaymentStatusMaster statusMaster;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFineAmount() { return fineAmount == null ? BigDecimal.ZERO : fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }
    public BigDecimal getTotalAmount() {
        return (amount == null ? BigDecimal.ZERO : amount).add(getFineAmount());
    }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Reciept getReciept() { return reciept; }
    public void setReciept(Reciept reciept) { this.reciept = reciept; }
    public MonthlyRentBill getMonthlyRentBill() { return monthlyRentBill; }
    public void setMonthlyRentBill(MonthlyRentBill monthlyRentBill) { this.monthlyRentBill = monthlyRentBill; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public DepositRefund getDepositRefund() { return depositRefund; }
    public void setDepositRefund(DepositRefund depositRefund) { this.depositRefund = depositRefund; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getStatus() {
        Long id = statusId == null ? 2L : statusId;
        if (id == 1L) return "PAID";
        if (id == 3L) return "PARTIAL";
        if (id == 4L) return "VOID";
        return "UNPAID";
    }
    public String getStatusLabel() {
        if (statusMaster != null && statusMaster.getName() != null) return statusMaster.getName();
        return switch (getStatus()) {
            case "PAID" -> "ชำระแล้ว";
            case "PARTIAL" -> "ชำระบางส่วน";
            case "VOID" -> "ยกเลิก";
            default -> "ค้างชำระ";
        };
    }
    public void setStatus(String status) {
        this.statusId = switch (status == null ? "UNPAID" : status) {
            case "PAID" -> 1L; case "PARTIAL" -> 3L; case "VOID" -> 4L; default -> 2L;
        };
    }
    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }
    public PaymentStatusMaster getStatusMaster() { return statusMaster; }
    public void setStatusMaster(PaymentStatusMaster statusMaster) { this.statusMaster = statusMaster; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
