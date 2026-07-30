package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "t_monthly_rent_bill",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_monthly_rent_bill_room_period",
                        columnNames = {"ID_room", "billing_month", "billing_year"}
                ),
                @UniqueConstraint(
                        name = "uk_monthly_rent_bill_bill_number",
                        columnNames = {"bill_number"}
                )
        }
)
public class MonthlyRentBill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_monthly_rent_bill")
    private Long id;

    @Column(name = "bill_number", length = 20, unique = true)
    private String billNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ID_room", referencedColumnName = "`ID_room`")
    private Room room;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ID_guest", referencedColumnName = "`ID_guest`")
    private Guest guest;

    @Column(name = "billing_month", nullable = false)
    private Integer billingMonth;

    @Column(name = "billing_year", nullable = false)
    private Integer billingYear;

    @Column(name = "rent_amount")
    private BigDecimal rentAmount = BigDecimal.ZERO;

    @Column(name = "previous_water_meter")
    private BigDecimal previousWaterMeter = BigDecimal.ZERO;

    @Column(name = "current_water_meter")
    private BigDecimal currentWaterMeter = BigDecimal.ZERO;

    @Column(name = "water_unit")
    private BigDecimal waterUnit = BigDecimal.ZERO;

    @Column(name = "water_rate")
    private BigDecimal waterRate = BigDecimal.ZERO;

    @Column(name = "water_amount")
    private BigDecimal waterAmount = BigDecimal.ZERO;

    @Column(name = "previous_electric_meter")
    private BigDecimal previousElectricMeter = BigDecimal.ZERO;

    @Column(name = "current_electric_meter")
    private BigDecimal currentElectricMeter = BigDecimal.ZERO;

    @Column(name = "electric_unit")
    private BigDecimal electricUnit = BigDecimal.ZERO;

    @Column(name = "electric_rate")
    private BigDecimal electricRate = BigDecimal.ZERO;

    @Column(name = "electric_amount")
    private BigDecimal electricAmount = BigDecimal.ZERO;

    @Column(name = "other_amount")
    private BigDecimal otherAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "subtotal_amount")
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "advance_applied_amount")
    private BigDecimal advanceAppliedAmount = BigDecimal.ZERO;

    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount")
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "id_bill_status")
    private Long statusId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_bill_status", insertable = false, updatable = false)
    private BillStatus billStatus;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        recalculate();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        recalculate();
    }

    public void recalculate() {
        previousWaterMeter = money(previousWaterMeter);
        currentWaterMeter = money(currentWaterMeter);
        waterRate = money(waterRate);
        previousElectricMeter = money(previousElectricMeter);
        currentElectricMeter = money(currentElectricMeter);
        electricRate = money(electricRate);
        rentAmount = money(rentAmount);
        otherAmount = money(otherAmount);
        discountAmount = money(discountAmount);
        advanceAppliedAmount = money(advanceAppliedAmount);
        paidAmount = money(paidAmount);
        waterUnit = currentWaterMeter.subtract(previousWaterMeter).max(BigDecimal.ZERO);
        electricUnit = currentElectricMeter.subtract(previousElectricMeter).max(BigDecimal.ZERO);
        waterAmount = waterUnit.multiply(waterRate);
        electricAmount = electricUnit.multiply(electricRate);
        subtotalAmount = rentAmount.add(waterAmount).add(electricAmount).add(otherAmount).subtract(discountAmount).max(BigDecimal.ZERO);
        advanceAppliedAmount = advanceAppliedAmount.min(subtotalAmount).max(BigDecimal.ZERO);
        totalAmount = subtotalAmount.subtract(advanceAppliedAmount).max(BigDecimal.ZERO);
        remainingAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }
    public String getDisplayBillNumber() { return billNumber == null || billNumber.isBlank() ? String.valueOf(id) : billNumber; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }
    public Integer getBillingMonth() { return billingMonth; }
    public void setBillingMonth(Integer billingMonth) { this.billingMonth = billingMonth; }
    public Integer getBillingYear() { return billingYear; }
    public void setBillingYear(Integer billingYear) { this.billingYear = billingYear; }
    public BigDecimal getRentAmount() { return rentAmount; }
    public void setRentAmount(BigDecimal rentAmount) { this.rentAmount = rentAmount; }
    public BigDecimal getPreviousWaterMeter() { return previousWaterMeter; }
    public void setPreviousWaterMeter(BigDecimal previousWaterMeter) { this.previousWaterMeter = previousWaterMeter; }
    public BigDecimal getCurrentWaterMeter() { return currentWaterMeter; }
    public void setCurrentWaterMeter(BigDecimal currentWaterMeter) { this.currentWaterMeter = currentWaterMeter; }
    public BigDecimal getWaterUnit() { return waterUnit; }
    public void setWaterUnit(BigDecimal waterUnit) { this.waterUnit = waterUnit; }
    public BigDecimal getWaterRate() { return waterRate; }
    public void setWaterRate(BigDecimal waterRate) { this.waterRate = waterRate; }
    public BigDecimal getWaterAmount() { return waterAmount; }
    public void setWaterAmount(BigDecimal waterAmount) { this.waterAmount = waterAmount; }
    public BigDecimal getPreviousElectricMeter() { return previousElectricMeter; }
    public void setPreviousElectricMeter(BigDecimal previousElectricMeter) { this.previousElectricMeter = previousElectricMeter; }
    public BigDecimal getCurrentElectricMeter() { return currentElectricMeter; }
    public void setCurrentElectricMeter(BigDecimal currentElectricMeter) { this.currentElectricMeter = currentElectricMeter; }
    public BigDecimal getElectricUnit() { return electricUnit; }
    public void setElectricUnit(BigDecimal electricUnit) { this.electricUnit = electricUnit; }
    public BigDecimal getElectricRate() { return electricRate; }
    public void setElectricRate(BigDecimal electricRate) { this.electricRate = electricRate; }
    public BigDecimal getElectricAmount() { return electricAmount; }
    public void setElectricAmount(BigDecimal electricAmount) { this.electricAmount = electricAmount; }
    public BigDecimal getOtherAmount() { return otherAmount; }
    public void setOtherAmount(BigDecimal otherAmount) { this.otherAmount = otherAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }
    public BigDecimal getAdvanceAppliedAmount() { return advanceAppliedAmount; }
    public void setAdvanceAppliedAmount(BigDecimal advanceAppliedAmount) { this.advanceAppliedAmount = advanceAppliedAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }
    public BillStatus getBillStatus() { return billStatus; }
    public void setBillStatus(BillStatus billStatus) {
        this.billStatus = billStatus;
        this.statusId = billStatus == null ? null : billStatus.getId();
    }
    public String getStatus() {
        return statusId == null ? null : switch (statusId.intValue()) { case 1 -> "PENDING"; case 2 -> "PARTIAL_PAID"; case 3 -> "PAID"; case 4 -> "CANCELLED"; default -> null; };
    }
    public void setStatus(String status) {
        this.statusId = switch (status == null ? "PENDING" : status) { case "PARTIAL_PAID" -> 2L; case "PAID" -> 3L; case "CANCELLED" -> 4L; default -> 1L; };
    }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
