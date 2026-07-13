package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "region_code")
public class RegionCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private RegionCode parent;

    @Column(name = "region_code", nullable = false, unique = true, length = 30)
    private String regionCode;

    @Column(nullable = false, length = 50)
    private String province;

    @Column(length = 50)
    private String city;

    @Column(name = "region_level", nullable = false, length = 30)
    private String regionLevel;

    protected RegionCode() {
    }

    public RegionCode(RegionCode parent, String regionCode, String province, String city, String regionLevel) {
        this.parent = parent;
        this.regionCode = regionCode;
        this.province = province;
        this.city = city;
        this.regionLevel = regionLevel;
    }

    public Integer getId() {
        return id;
    }

    public RegionCode getParent() {
        return parent;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getRegionLevel() {
        return regionLevel;
    }

    public boolean update(RegionCode parent, String province, String city, String regionLevel) {
        Integer thisParentId = this.parent == null ? null : this.parent.getId();
        Integer nextParentId = parent == null ? null : parent.getId();
        boolean changed = !java.util.Objects.equals(thisParentId, nextParentId)
                || !java.util.Objects.equals(this.province, province)
                || !java.util.Objects.equals(this.city, city)
                || !java.util.Objects.equals(this.regionLevel, regionLevel);
        this.parent = parent;
        this.province = province;
        this.city = city;
        this.regionLevel = regionLevel;
        return changed;
    }

    public String displayName() {
        return city == null || city.isBlank() ? province : province + " " + city;
    }
}
