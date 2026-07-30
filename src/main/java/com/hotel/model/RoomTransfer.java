package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_room_transfer")
public class RoomTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_room_transfer`")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "`ID_guest`", referencedColumnName = "`ID_guest`")
    private Guest guest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "`ID_from_room`", referencedColumnName = "`ID_room`")
    private Room fromRoom;

    @ManyToOne(optional = false)
    @JoinColumn(name = "`ID_to_room`", referencedColumnName = "`ID_room`")
    private Room toRoom;

    @Column(name = "transfer_date")
    private LocalDate transferDate = LocalDate.now();

    @Column(name = "id_stay_type", nullable = false)
    private Long stayTypeId = 1L;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_stay_type", insertable = false, updatable = false)
    private StayTypeMaster stayTypeMaster;

    @Column(name = "old_price")
    private BigDecimal oldPrice = BigDecimal.ZERO;

    @Column(name = "new_price")
    private BigDecimal newPrice = BigDecimal.ZERO;

    @Column(name = "remark", length = 1000)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }
    public Room getFromRoom() { return fromRoom; }
    public void setFromRoom(Room fromRoom) { this.fromRoom = fromRoom; }
    public Room getToRoom() { return toRoom; }
    public void setToRoom(Room toRoom) { this.toRoom = toRoom; }
    public LocalDate getTransferDate() { return transferDate; }
    public void setTransferDate(LocalDate transferDate) { this.transferDate = transferDate; }
    public String getStayType() { return stayTypeMaster != null ? stayTypeMaster.getCode() : (stayTypeId != null && stayTypeId == 2L ? "MONTHLY" : "DAILY"); }
    public void setStayType(String stayType) { this.stayTypeId = "MONTHLY".equals(stayType) ? 2L : 1L; }
    public Long getStayTypeId() { return stayTypeId; }
    public void setStayTypeId(Long id) { this.stayTypeId = id; }
    public StayTypeMaster getStayTypeMaster() { return stayTypeMaster; }
    public void setStayTypeMaster(StayTypeMaster value) { this.stayTypeMaster = value; }
    public BigDecimal getOldPrice() { return oldPrice; }
    public void setOldPrice(BigDecimal oldPrice) { this.oldPrice = oldPrice; }
    public BigDecimal getNewPrice() { return newPrice; }
    public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
