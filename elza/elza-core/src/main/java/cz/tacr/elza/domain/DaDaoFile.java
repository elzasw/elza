package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigInteger;

@Entity(name = "da_dao_file")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class DaDaoFile {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer daoFileId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaDao.class)
    @JoinColumn(name = "dao_id", nullable = false)
    private DaDao dao;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private DaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "delete_change_id")
    private DaChange deleteChange;

    @Column(name = "checksum", length = StringLength.LENGTH_250)
    private String checksum;

    @Column(name = "checksum_type", length = StringLength.LENGTH_10)
    private String checksumType;

    @Column(name = "mime_type", length = StringLength.LENGTH_50)
    private String mimeType;

    @Column(name = "size")
    private BigInteger size;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "source_x_dimension_unit", length = StringLength.LENGTH_10)
    private String sourceXDimensionUnit;

    @Column(name = "source_x_dimension_value")
    private Integer sourceXDimensionValue;

    @Column(name = "source_y_dimension_unit", length = StringLength.LENGTH_10)
    private String sourceYDimensionUnit;

    @Column(name = "source_y_dimension_value")
    private Integer sourceYDimensionValue;

    @Column(name = "duration", length = StringLength.LENGTH_250)
    private String duration;

    @Column(name = "description", length = StringLength.LENGTH_2000)
    private String description;

    @Column(name = "file_name", length = StringLength.LENGTH_1000)
    private String fileName;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaDaoFileFolder.class)
    @JoinColumn(name = "dao_file_folder_id")
    private DaDaoFileFolder daoFileFolder;

    public Integer getDaoFileId() {
        return daoFileId;
    }

    public void setDaoFileId(Integer daoFileId) {
        this.daoFileId = daoFileId;
    }

    public DaDao getDao() {
        return dao;
    }

    public void setDao(DaDao dao) {
        this.dao = dao;
    }

    public DaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(DaChange createChange) {
        this.createChange = createChange;
    }

    public DaChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(DaChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getChecksumType() {
        return checksumType;
    }

    public void setChecksumType(String checksumType) {
        this.checksumType = checksumType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public BigInteger getSize() {
        return size;
    }

    public void setSize(BigInteger size) {
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

    public String getSourceXDimensionUnit() {
        return sourceXDimensionUnit;
    }

    public void setSourceXDimensionUnit(String sourceXDimensionUnit) {
        this.sourceXDimensionUnit = sourceXDimensionUnit;
    }

    public Integer getSourceXDimensionValue() {
        return sourceXDimensionValue;
    }

    public void setSourceXDimensionValue(Integer sourceXDimensionValue) {
        this.sourceXDimensionValue = sourceXDimensionValue;
    }

    public String getSourceYDimensionUnit() {
        return sourceYDimensionUnit;
    }

    public void setSourceYDimensionUnit(String sourceYDimensionUnit) {
        this.sourceYDimensionUnit = sourceYDimensionUnit;
    }

    public Integer getSourceYDimensionValue() {
        return sourceYDimensionValue;
    }

    public void setSourceYDimensionValue(Integer sourceYDimensionValue) {
        this.sourceYDimensionValue = sourceYDimensionValue;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
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

    public DaDaoFileFolder getDaoFileFolder() {
        return daoFileFolder;
    }

    public void setDaoFileFolder(DaDaoFileFolder daoFileFolder) {
        this.daoFileFolder = daoFileFolder;
    }
}
