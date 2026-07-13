package com.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "t_daily_checkout_penalty_rule")
public class DailyCheckoutPenaltyRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_daily_checkout_penalty_rule`")
    private Long id;

    @Column(name = "start_day_offset", nullable = false)
    private Integer startDayOffset = 0;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_day_offset")
    private Integer endDayOffset;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 40)
    private DailyCheckoutPenaltyChargeType chargeType = DailyCheckoutPenaltyChargeType.FIXED;

    @Column(name = "charge_value", nullable = false)
    private BigDecimal chargeValue = BigDecimal.ZERO;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getStartDayOffset() { return startDayOffset; }
    public void setStartDayOffset(Integer startDayOffset) { this.startDayOffset = startDayOffset; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public Integer getEndDayOffset() { return endDayOffset; }
    public void setEndDayOffset(Integer endDayOffset) { this.endDayOffset = endDayOffset; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public DailyCheckoutPenaltyChargeType getChargeType() { return chargeType; }
    public void setChargeType(DailyCheckoutPenaltyChargeType chargeType) { this.chargeType = chargeType; }
    public BigDecimal getChargeValue() { return chargeValue; }
    public void setChargeValue(BigDecimal chargeValue) { this.chargeValue = chargeValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
