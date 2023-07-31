package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import cz.tacr.elza.api.UnitOfMeasure;
import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Soubor k DAO.
 *
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao_file")
public class ArrDaoFile {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer daoFileId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId", nullable = false)
    private ArrDao dao;

    @Column(name = "daoId", nullable = false, insertable = false, updatable = false)
    private Integer daoId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDaoFileGroup.class)
    @JoinColumn(name = "daoFileGroupId")
    private ArrDaoFileGroup daoFileGroup;

    @Column(nullable = false, length = StringLength.LENGTH_250)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column
    private ChecksumType checksumType;

    @Column
    private LocalDateTime created;

    @Column(length = StringLength.LENGTH_50)
    private String mimetype;

    @Column
    private Long size;

    @Column
    private Integer imageHeight;

    @Column
    private Integer imageWidth;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_x_dimension_unit")
    private UnitOfMeasure sourceXDimesionUnit;

    @Column(name = "source_x_dimension_value")
    private Double sourceXDimesionValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_y_dimension_unit")
    private UnitOfMeasure sourceYDimesionUnit;

    @Column(name = "source_y_dimension_value")
    private Double sourceYDimesionValue;

    @Column(length = StringLength.LENGTH_250)
    private String duration;

    @Column(nullable = false, length = StringLength.LENGTH_1000, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_2000)
    private String description;

    @Column(length = StringLength.LENGTH_1000)
    private String fileName;

    public Integer getDaoFileId() {
        return daoFileId;
    }

    public void setDaoFileId(final Integer daoFileId) {
        this.daoFileId = daoFileId;
    }

    public ArrDao getDao() {
        return dao;
    }

    public void setDao(final ArrDao dao) {
        this.dao = dao;
        this.daoId = dao == null ? null : dao.getDaoId();
    }

    public Integer getDaoId() {
        if (daoId == null && dao != null) {
            return dao.getDaoId();
        }
        return daoId;
    }

    public ArrDaoFileGroup getDaoFileGroup() {
        return daoFileGroup;
    }

    public void setDaoFileGroup(final ArrDaoFileGroup daoFileGroup) {
        this.daoFileGroup = daoFileGroup;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(final String checksum) {
        this.checksum = checksum;
    }

    public ChecksumType getChecksumType() {
        return checksumType;
    }

    public void setChecksumType(final ChecksumType checksumType) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public enum ChecksumType {
        MD5,
        SHA1,
        SHA256,
        SHA384,
        SHA512,
    }
}
