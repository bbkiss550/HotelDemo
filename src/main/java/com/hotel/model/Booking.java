package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "t_booking")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_booking`")
    private Long id;

    @Column(name = "b_customer_name", nullable = false)
    private String customerName;

    @Column(name = "b_phone")
    private String phone;

    @Column(name = "b_booking_number", unique = true)
    private String bookingNumber;

    @Column(name = "b_id_card")
    private String idCard;

    @Column(name = "b_nationality")
    private String nationality;

    @ManyToOne
    @JoinColumn(name = "`ID_room`", referencedColumnName = "`ID_room`")
    private Room room;

    @ManyToOne
    @JoinColumn(name = "`ID_room_type`", referencedColumnName = "`ID_room_type`")
    private RoomType roomType;

    @Column(name = "b_booking_date")
    private LocalDate bookingDate = LocalDate.now();

    @Column(name = "b_check_in_date")
    private LocalDate checkInDate;

    @Column(name = "b_check_out_date")
    private LocalDate checkOutDate;

    @Column(name = "id_stay_type", nullable = false)
    private Long stayTypeId = 1L;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_stay_type", insertable = false, updatable = false)
    private StayTypeMaster stayTypeMaster;

    @Column(name = "b_deposit_amount")
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "id_booking_status", nullable = false)
    private Long statusId = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_booking_status", insertable = false, updatable = false)
    private BookingStatusMaster statusMaster;

    @Column(name = "b_note", length = 1000)
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBookingNumber() { return bookingNumber; }
    public void setBookingNumber(String bookingNumber) { this.bookingNumber = bookingNumber; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }
    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    public String getStayType() { return stayTypeMaster != null ? stayTypeMaster.getCode() : (stayTypeId != null && stayTypeId == 2L ? "MONTHLY" : "DAILY"); }
    public String getStayTypeLabel() {
        if (stayTypeMaster != null && stayTypeMaster.getName() != null) return stayTypeMaster.getName();
        return "MONTHLY".equals(getStayType()) ? "รายเดือน" : "รายวัน";
    }
    public void setStayType(String stayType) { this.stayTypeId = "MONTHLY".equals(stayType) ? 2L : 1L; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public String getStatus() { return statusMaster != null ? statusMaster.getCode() : (statusId != null && statusId == 3L ? "CHECKED_IN" : statusId != null && statusId == 4L ? "CANCELLED" : "PENDING"); }
    public String getStatusLabel() {
        if (statusMaster != null && statusMaster.getName() != null) return statusMaster.getName();
        return switch (getStatus()) { case "CHECKED_IN" -> "เข้าพักแล้ว"; case "CANCELLED" -> "ยกเลิก"; default -> "รอเข้าพัก"; };
    }
    public void setStatus(String status) { this.statusId = switch (status == null ? "PENDING" : status) { case "CHECKED_IN" -> 3L; case "CANCELLED" -> 4L; default -> 1L; }; }
    public Long getStayTypeId() { return stayTypeId; }
    public void setStayTypeId(Long id) { this.stayTypeId = id; }
    public StayTypeMaster getStayTypeMaster() { return stayTypeMaster; }
    public void setStayTypeMaster(StayTypeMaster value) { this.stayTypeMaster = value; }
    public Long getStatusId() { return statusId; }
    public void setStatusId(Long id) { this.statusId = id; }
    public BookingStatusMaster getStatusMaster() { return statusMaster; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
