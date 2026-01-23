package com.aidoctor.diagnosis.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * CDP版本实体类
 */
@Entity
@Table(name = "cdp_version")
public class CDPVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "cdp_id", length = 64)
    private String cdpId;
    
    @Column(name = "version_number")
    private Integer versionNumber;
    
    @Column(name = "cdp_data", columnDefinition = "LONGTEXT")
    private String cdpDataJson;  // CDP数据的JSON序列化字符串
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCdpId() {
        return cdpId;
    }
    
    public void setCdpId(String cdpId) {
        this.cdpId = cdpId;
    }
    
    public Integer getVersionNumber() {
        return versionNumber;
    }
    
    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }
    
    public String getCdpDataJson() {
        return cdpDataJson;
    }
    
    public void setCdpDataJson(String cdpDataJson) {
        this.cdpDataJson = cdpDataJson;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

