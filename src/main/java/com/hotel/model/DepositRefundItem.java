package com.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "t_deposit_refund_item")
public class DepositRefundItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_deposit_refund_item`")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "`ID_deposit_refund`", referencedColumnName = "`ID_deposit_refund`")
    private DepositRefund refund;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "item_name_en")
    private String itemNameEn;

    @Column(name = "item_amount", nullable = false)
    private BigDecimal itemAmount = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DepositRefund getRefund() { return refund; }
    public void setRefund(DepositRefund refund) { this.refund = refund; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemNameEn() { return itemNameEn; }
    public void setItemNameEn(String itemNameEn) { this.itemNameEn = itemNameEn; }
    public BigDecimal getItemAmount() { return itemAmount; }
    public void setItemAmount(BigDecimal itemAmount) { this.itemAmount = itemAmount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
