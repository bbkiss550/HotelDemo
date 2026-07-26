package com.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "t_menu")
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID_menu`")
    private Long id;

    @Column(name = "m_icon", nullable = false)
    private String icon;

    @Column(name = "m_name", nullable = false)
    private String name;

    @Column(name = "m_link", unique = true)
    private String link;

    @Column(name = "m_sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "m_status", nullable = false, length = 1)
    private String status = "A";

    @Column(name = "`ID_parent_menu`")
    private Long parentId;

    @Transient
    private List<Menu> children = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public List<Menu> getChildren() { return children; }
    public void setChildren(List<Menu> children) { this.children = children == null ? new ArrayList<>() : children; }
    public boolean hasChildren() { return children != null && !children.isEmpty(); }
}
