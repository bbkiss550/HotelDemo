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

    @Enumerated(EnumType.STRING)
    @Column(name = "b_stay_type")
    private StayType stayType = StayType.DAILY;

    @Column(name = "b_deposit_amount")
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "b_status")
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(name = "b_note", length = 1000)
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
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
    public StayType getStayType() { return stayType; }
    public void setStayType(StayType stayType) { this.stayType = stayType; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
