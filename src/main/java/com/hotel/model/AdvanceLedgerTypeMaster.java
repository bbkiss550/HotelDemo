package com.hotel.model;
import jakarta.persistence.*;
@Entity @Table(name="t_advance_ledger_type")
public class AdvanceLedgerTypeMaster {
    @Id @Column(name="id_advance_ledger_type") private Long id;
    @Column(name="alt_code", nullable=false, unique=true) private String code;
    @Column(name="alt_name", nullable=false) private String name;
    @Column(name="alt_name_en") private String nameEn;
    @Column(name="is_active", nullable=false) private boolean active=true;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getNameEn(){return nameEn;} public void setNameEn(String v){nameEn=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
