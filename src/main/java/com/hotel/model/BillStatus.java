package com.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_bill_status")
public class BillStatus {
    @Id
    @Column(name = "id_bill_status")
    private Long id;

    @Column(name = "bs_name", nullable = false, length = 100)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
