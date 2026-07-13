package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "region_external_code")
public class RegionExternalCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private RegionCode region;

    @Column(name = "code_system", nullable = false, length = 50)
    private String codeSystem;

    @Column(name = "external_code", nullable = false, length = 50)
    private String externalCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RegionExternalCode() {
    }

    public RegionExternalCode(RegionCode region, String codeSystem, String externalCode) {
        this.region = region;
        this.codeSystem = codeSystem;
        this.externalCode = externalCode;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void touch(RegionCode region) {
        this.region = region;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public RegionCode getRegion() { return region; }
    public String getCodeSystem() { return codeSystem; }
    public String getExternalCode() { return externalCode; }
}
