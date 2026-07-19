package com.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_reciept_type")
public class RecieptType {
    public static final long OPENING_MONTHLY = 1L;
    public static final long DAILY_SERVICE = 2L;
    public static final long MONTHLY_RENT = 3L;
    public static final long BOOKING_DEPOSIT = 4L;
    public static final long PENALTY = 5L;

    @Id
    @Column(name = "`ID_rec_type`")
    private Long id;

    @Column(name = "rec_type_name", nullable = false)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
