package com.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "t_room_type")
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_room_type`")
    private Long id;

    @Column(name = "rt_name", nullable = false, unique = true)
    private String name;

    @Column(name = "rt_nightly_price")
    private BigDecimal nightlyPrice = BigDecimal.ZERO;

    @Column(name = "rt_monthly_price")
    private BigDecimal monthlyPrice = BigDecimal.ZERO;

    @Column(name = "rt_detail", length = 1000)
    private String detail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getNightlyPrice() { return nightlyPrice; }
    public void setNightlyPrice(BigDecimal nightlyPrice) { this.nightlyPrice = nightlyPrice; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
