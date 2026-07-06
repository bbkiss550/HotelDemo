package com.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "t_reciept")
public class Reciept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_reciept`")
    private Long id;

    @Column(name = "rec_date", nullable = false)
    private LocalDate recieptDate = LocalDate.now();

    @Column(name = "rec_no", length = 30, unique = true)
    private String recieptNo;

    @ManyToOne(optional = false)
    @jakarta.persistence.JoinColumn(name = "`ID_rec_type`", referencedColumnName = "`ID_rec_type`")
    private RecieptType type;

    @Column(name = "rec_amount", nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getRecieptDate() { return recieptDate; }
    public void setRecieptDate(LocalDate recieptDate) { this.recieptDate = recieptDate; }
    public String getRecieptNo() { return recieptNo; }
    public void setRecieptNo(String recieptNo) { this.recieptNo = recieptNo; }
    public RecieptType getType() { return type; }
    public void setType(RecieptType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
