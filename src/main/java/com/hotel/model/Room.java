package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "t_room")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_room`")
    private Long id;
    @Column(name = "r_room_number", nullable = false, unique = true)
    private String roomNumber;
    @ManyToOne
    @JoinColumn(name = "`ID_room_type`", referencedColumnName = "`ID_room_type`")
    private RoomType roomType;
    @Column(name = "r_room_type", insertable = false, updatable = false)
    private String legacyRoomType;
    @ManyToOne
    @JoinColumn(name = "`ID_floor`", referencedColumnName = "`ID_floor`")
    private Floor floor;
    @Column(name = "r_floor", insertable = false, updatable = false)
    private Integer legacyFloor;
    @Column(name = "r_nightly_price")
    private BigDecimal nightlyPrice = BigDecimal.ZERO;
    @Column(name = "r_monthly_price")
    private BigDecimal monthlyPrice = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(name = "r_status")
    private RoomStatus status = RoomStatus.AVAILABLE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }
    public String getLegacyRoomType() { return legacyRoomType; }
    public Floor getFloor() { return floor; }
    public void setFloor(Floor floor) { this.floor = floor; }
    public Integer getLegacyFloor() { return legacyFloor; }
    public BigDecimal getNightlyPrice() { return nightlyPrice; }
    public void setNightlyPrice(BigDecimal nightlyPrice) { this.nightlyPrice = nightlyPrice; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
}
