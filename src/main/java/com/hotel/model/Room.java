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
    @Column(name = "id_room_status", nullable = false)
    private Long statusId = 1L;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_room_status", insertable = false, updatable = false)
    private RoomStatusMaster statusMaster;

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
    public String getStatus() {
        if (statusMaster != null && statusMaster.getCode() != null) return statusMaster.getCode();
        Long id = statusId == null ? 1L : statusId;
        if (id == 2L) return "OCCUPIED";
        if (id == 3L) return "DAILY_OCCUPIED";
        if (id == 4L) return "MONTHLY_OCCUPIED";
        if (id == 5L) return "RESERVED";
        if (id == 6L) return "MAINTENANCE";
        return "AVAILABLE";
    }
    public String getStatusLabel() {
        if (statusMaster != null && statusMaster.getName() != null) return statusMaster.getName();
        return switch (getStatus()) {
            case "OCCUPIED" -> "มีผู้เข้าพัก";
            case "DAILY_OCCUPIED" -> "พักรายวัน";
            case "MONTHLY_OCCUPIED" -> "พักรายเดือน";
            case "RESERVED" -> "จองแล้ว";
            case "MAINTENANCE" -> "ปิดปรับปรุง";
            default -> "ว่าง";
        };
    }
    public void setStatus(String status) {
        String code = status == null ? "AVAILABLE" : status;
        this.statusId = switch (code) { case "OCCUPIED" -> 2L; case "DAILY_OCCUPIED" -> 3L; case "MONTHLY_OCCUPIED" -> 4L; case "RESERVED" -> 5L; case "MAINTENANCE" -> 6L; default -> 1L; };
    }
    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }
    public RoomStatusMaster getStatusMaster() { return statusMaster; }
    public void setStatusMaster(RoomStatusMaster statusMaster) { this.statusMaster = statusMaster; }
}
