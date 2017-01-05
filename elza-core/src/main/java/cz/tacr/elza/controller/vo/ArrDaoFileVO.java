package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.ArrDaoFile;
import cz.tacr.elza.api.UnitOfMeasure;

import java.time.LocalDateTime;

/**
 * Value objekt {@link ArrDaoFile}
 *
 * @author Martin Lebeda
 * @since 13.12.2016
 */
public class ArrDaoFileVO {

    private Integer id;
//    private ArrDaoVO dao;
//    private ArrDaoFileGroupVO daoFileGroup;
    private String checksum;
    private ArrDaoFile.ChecksumType checksumType;
    private LocalDateTime created;
    private String mimetype;
    private Long size;
    private Integer imageHeight;
    private Integer imageWidth;
    private UnitOfMeasure sourceXDimesionUnit;
    private Double sourceXDimesionValue;
    private UnitOfMeasure sourceYDimesionUnit;
    private Double sourceYDimesionValue;
    private String duration;
    private String code;
    private String url;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public ArrDaoFile.ChecksumType getChecksumType() {
        return checksumType;
    }

    public void setChecksumType(ArrDaoFile.ChecksumType checksumType) {
        this.checksumType = checksumType;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public UnitOfMeasure getSourceXDimesionUnit() {
        return sourceXDimesionUnit;
    }

    public void setSourceXDimesionUnit(UnitOfMeasure sourceXDimesionUnit) {
        this.sourceXDimesionUnit = sourceXDimesionUnit;
    }

    public Double getSourceXDimesionValue() {
        return sourceXDimesionValue;
    }

    public void setSourceXDimesionValue(Double sourceXDimesionValue) {
        this.sourceXDimesionValue = sourceXDimesionValue;
    }

    public UnitOfMeasure getSourceYDimesionUnit() {
        return sourceYDimesionUnit;
    }

    public void setSourceYDimesionUnit(UnitOfMeasure sourceYDimesionUnit) {
        this.sourceYDimesionUnit = sourceYDimesionUnit;
    }

    public Double getSourceYDimesionValue() {
        return sourceYDimesionValue;
    }

    public void setSourceYDimesionValue(Double sourceYDimesionValue) {
        this.sourceYDimesionValue = sourceYDimesionValue;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
