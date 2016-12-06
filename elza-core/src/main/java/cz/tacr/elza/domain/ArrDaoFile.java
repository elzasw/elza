package cz.tacr.elza.domain;

import cz.tacr.elza.api.UnitOfMeasure;
import cz.tacr.elza.domain.enumeration.StringLength;

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
import java.time.LocalDateTime;


/**
 * Implementace {@link cz.tacr.elza.api.ArrDaoFile}
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao_file")
public class ArrDaoFile implements cz.tacr.elza.api.ArrDaoFile<ArrDao, ArrDaoFileGroup> {

    @Id
    @GeneratedValue
    private Integer daoFileId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId", nullable = false)
    private ArrDao dao;

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
    @Column
    private UnitOfMeasure sourceXDimesionUnit;

    @Column
    private Double sourceXDimesionValue;

    @Enumerated(EnumType.STRING)
    @Column
    private UnitOfMeasure sourceYDimesionUnit;

    @Column
    private Double sourceYDimesionValue;

    @Column(length = StringLength.LENGTH_250)
    private String duration;

    @Column(nullable = false, length = StringLength.LENGTH_50, unique = true)
    private String code;

    @Override
    public Integer getDaoFileId() {
        return daoFileId;
    }

    @Override
    public void setDaoFileId(final Integer daoFileId) {
        this.daoFileId = daoFileId;
    }

    @Override
    public ArrDao getDao() {
        return dao;
    }

    @Override
    public void setDao(final ArrDao dao) {
        this.dao = dao;
    }

    @Override
    public ArrDaoFileGroup getDaoFileGroup() {
        return daoFileGroup;
    }

    @Override
    public void setDaoFileGroup(final ArrDaoFileGroup daoFileGroup) {
        this.daoFileGroup = daoFileGroup;
    }

    @Override
    public String getChecksum() {
        return checksum;
    }

    @Override
    public void setChecksum(final String checksum) {
        this.checksum = checksum;
    }

    @Override
    public ChecksumType getChecksumType() {
        return checksumType;
    }

    @Override
    public void setChecksumType(final ChecksumType checksumType) {
        this.checksumType = checksumType;
    }

    @Override
    public LocalDateTime getCreated() {
        return created;
    }

    @Override
    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    @Override
    public String getMimetype() {
        return mimetype;
    }

    @Override
    public void setMimetype(final String mimetype) {
        this.mimetype = mimetype;
    }

    @Override
    public Long getSize() {
        return size;
    }

    @Override
    public void setSize(final Long size) {
        this.size = size;
    }

    @Override
    public Integer getImageHeight() {
        return imageHeight;
    }

    @Override
    public void setImageHeight(final Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    @Override
    public Integer getImageWidth() {
        return imageWidth;
    }

    @Override
    public void setImageWidth(final Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    @Override
    public UnitOfMeasure getSourceXDimesionUnit() {
        return sourceXDimesionUnit;
    }

    @Override
    public void setSourceXDimesionUnit(final UnitOfMeasure sourceXDimesionUnit) {
        this.sourceXDimesionUnit = sourceXDimesionUnit;
    }

    @Override
    public Double getSourceXDimesionValue() {
        return sourceXDimesionValue;
    }

    @Override
    public void setSourceXDimesionValue(final Double sourceXDimesionValue) {
        this.sourceXDimesionValue = sourceXDimesionValue;
    }

    @Override
    public UnitOfMeasure getSourceYDimesionUnit() {
        return sourceYDimesionUnit;
    }

    @Override
    public void setSourceYDimesionUnit(final UnitOfMeasure sourceYDimesionUnit) {
        this.sourceYDimesionUnit = sourceYDimesionUnit;
    }

    @Override
    public Double getSourceYDimesionValue() {
        return sourceYDimesionValue;
    }

    @Override
    public void setSourceYDimesionValue(final Double sourceYDimesionValue) {
        this.sourceYDimesionValue = sourceYDimesionValue;
    }

    @Override
    public String getDuration() {
        return duration;
    }

    @Override
    public void setDuration(final String duration) {
        this.duration = duration;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }
}
