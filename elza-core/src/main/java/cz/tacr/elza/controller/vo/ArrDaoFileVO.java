package cz.tacr.elza.controller.vo;

import java.time.LocalDateTime;

import cz.tacr.elza.api.UnitOfMeasure;
import cz.tacr.elza.domain.ArrDaoFile;

/**
 * Value objekt {@link ArrDaoFile}
 *
 * @author Martin Lebeda
 * @since 13.12.2016
 */
public class ArrDaoFileVO {

    private Integer id;
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
    private String thumbnailUrl;
    private String description;
    private String fileName;

    public ArrDaoFileVO() {

    }

    public ArrDaoFileVO(final ArrDaoFile daoFile) {
        id = daoFile.getDaoFileId();
        checksum = daoFile.getChecksum();
        checksumType = daoFile.getChecksumType();
        created = daoFile.getCreated();
        mimetype = daoFile.getMimetype();
        size = daoFile.getSize();
        imageHeight = daoFile.getImageHeight();
        imageWidth = daoFile.getImageWidth();
        sourceXDimesionUnit = daoFile.getSourceXDimesionUnit();
        sourceXDimesionValue = daoFile.getSourceXDimesionValue();
        sourceYDimesionUnit = daoFile.getSourceYDimesionUnit();
        sourceYDimesionValue = daoFile.getSourceYDimesionValue();
        duration = daoFile.getDuration();
        code = daoFile.getCode();
        description = daoFile.getDescription();
        fileName = daoFile.getFileName();
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(final String checksum) {
        this.checksum = checksum;
    }

    public ArrDaoFile.ChecksumType getChecksumType() {
        return checksumType;
    }

    public void setChecksumType(final ArrDaoFile.ChecksumType checksumType) {
        this.checksumType = checksumType;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(final String mimetype) {
        this.mimetype = mimetype;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(final Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(final Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public UnitOfMeasure getSourceXDimesionUnit() {
        return sourceXDimesionUnit;
    }

    public void setSourceXDimesionUnit(final UnitOfMeasure sourceXDimesionUnit) {
        this.sourceXDimesionUnit = sourceXDimesionUnit;
    }

    public Double getSourceXDimesionValue() {
        return sourceXDimesionValue;
    }

    public void setSourceXDimesionValue(final Double sourceXDimesionValue) {
        this.sourceXDimesionValue = sourceXDimesionValue;
    }

    public UnitOfMeasure getSourceYDimesionUnit() {
        return sourceYDimesionUnit;
    }

    public void setSourceYDimesionUnit(final UnitOfMeasure sourceYDimesionUnit) {
        this.sourceYDimesionUnit = sourceYDimesionUnit;
    }

    public Double getSourceYDimesionValue() {
        return sourceYDimesionValue;
    }

    public void setSourceYDimesionValue(final Double sourceYDimesionValue) {
        this.sourceYDimesionValue = sourceYDimesionValue;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(final String duration) {
        this.duration = duration;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public static ArrDaoFileVO newInstance(ArrDaoFile daoFile) {
        ArrDaoFileVO daoFileVo = new ArrDaoFileVO(daoFile);
        return daoFileVo;
    }
}
