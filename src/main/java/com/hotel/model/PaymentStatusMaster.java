package com.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_payment_status")
public class PaymentStatusMaster {
    @Id
    @Column(name = "id_payment_status")
    private Long id;
    @Column(name = "ps_code", nullable = false, unique = true, length = 30)
    private String code;
    @Column(name = "ps_name", nullable = false, length = 100)
    private String name;
    @Column(name = "ps_name_en", length = 100)
    private String nameEn;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
