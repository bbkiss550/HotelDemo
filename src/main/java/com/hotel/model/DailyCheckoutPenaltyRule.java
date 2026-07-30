package com.hotel.model;

import jakarta.persistence.*;
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

    @Column(name = "id_penalty_charge_type", nullable = false)
    private Long chargeTypeId = 2L;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_penalty_charge_type", insertable = false, updatable = false)
    private PenaltyChargeTypeMaster chargeTypeMaster;

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
    public String getChargeType() { return chargeTypeMaster != null ? chargeTypeMaster.getCode() : (chargeTypeId != null && chargeTypeId == 1L ? "FREE" : chargeTypeId != null && chargeTypeId == 3L ? "PER_HOUR" : chargeTypeId != null && chargeTypeId == 4L ? "PERCENT_DAILY_RATE" : chargeTypeId != null && chargeTypeId == 5L ? "ADD_NIGHTS" : "FIXED"); }
    public void setChargeType(String chargeType) { this.chargeTypeId = switch (chargeType == null ? "FIXED" : chargeType) { case "FREE" -> 1L; case "PER_HOUR" -> 3L; case "PERCENT_DAILY_RATE" -> 4L; case "ADD_NIGHTS" -> 5L; default -> 2L; }; }
    public Long getChargeTypeId() { return chargeTypeId; }
    public void setChargeTypeId(Long id) { this.chargeTypeId = id; }
    public PenaltyChargeTypeMaster getChargeTypeMaster() { return chargeTypeMaster; }
    public void setChargeTypeMaster(PenaltyChargeTypeMaster value) { this.chargeTypeMaster = value; }
    public BigDecimal getChargeValue() { return chargeValue; }
    public void setChargeValue(BigDecimal chargeValue) { this.chargeValue = chargeValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
