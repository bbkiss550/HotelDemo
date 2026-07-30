package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "t_guest")
public class Guest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_guest`")
    private Long id;

    @Column(name = "g_full_name")
    private String fullName;

    @Column(name = "g_phone")
    private String phone;

    @Column(name = "g_id_card")
    private String idCard;

    @Column(name = "g_nationality")
    private String nationality;

    @Column(name = "g_address", length = 1000)
    private String address;

    @ManyToOne(optional = false)
    @JoinColumn(name = "`ID_room`", referencedColumnName = "`ID_room`")
    private Room room;

    @Column(name = "g_check_in_date")
    private LocalDate checkInDate;

    @Column(name = "g_check_out_date")
    private LocalDate checkOutDate;

    @Column(name = "id_stay_type", nullable = false)
    private Long stayTypeId = 1L;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_stay_type", insertable = false, updatable = false)
    private StayTypeMaster stayTypeMaster;

    @Column(name = "g_price")
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "g_advance_months")
    private Integer advanceMonths;

    @Column(name = "g_deposit")
    private BigDecimal deposit = BigDecimal.ZERO;

    @Column(name = "g_initial_payment")
    private BigDecimal initialPayment = BigDecimal.ZERO;

    @Column(name = "g_total_paid")
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "g_advance_balance")
    private BigDecimal advanceBalance = BigDecimal.ZERO;

    @Column(name = "g_initial_water_meter")
    private BigDecimal initialWaterMeter = BigDecimal.ZERO;

    @Column(name = "g_initial_electric_meter")
    private BigDecimal initialElectricMeter = BigDecimal.ZERO;

    @Column(name = "g_active")
    private Boolean active = true;

    @Column(name = "g_note", length = 1000)
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    public String getStayType() { return stayTypeMaster != null ? stayTypeMaster.getCode() : (stayTypeId != null && stayTypeId == 2L ? "MONTHLY" : "DAILY"); }
    public String getStayTypeLabel() { return stayTypeMaster != null && stayTypeMaster.getName() != null ? stayTypeMaster.getName() : ("MONTHLY".equals(getStayType()) ? "รายเดือน" : "รายวัน"); }
    public void setStayType(String stayType) { this.stayTypeId = "MONTHLY".equals(stayType) ? 2L : 1L; }
    public Long getStayTypeId() { return stayTypeId; }
    public void setStayTypeId(Long id) { this.stayTypeId = id; }
    public StayTypeMaster getStayTypeMaster() { return stayTypeMaster; }
    public void setStayTypeMaster(StayTypeMaster value) { this.stayTypeMaster = value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getAdvanceMonths() { return advanceMonths; }
    public void setAdvanceMonths(Integer advanceMonths) { this.advanceMonths = advanceMonths; }
    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }
    public BigDecimal getInitialPayment() { return initialPayment; }
    public void setInitialPayment(BigDecimal initialPayment) { this.initialPayment = initialPayment; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
    public BigDecimal getAdvanceBalance() { return advanceBalance; }
    public void setAdvanceBalance(BigDecimal advanceBalance) { this.advanceBalance = advanceBalance; }
    public BigDecimal getInitialWaterMeter() { return initialWaterMeter; }
    public void setInitialWaterMeter(BigDecimal initialWaterMeter) { this.initialWaterMeter = initialWaterMeter; }
    public BigDecimal getInitialElectricMeter() { return initialElectricMeter; }
    public void setInitialElectricMeter(BigDecimal initialElectricMeter) { this.initialElectricMeter = initialElectricMeter; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
