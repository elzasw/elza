package cz.tacr.elza.controller.vo;

import java.math.BigInteger;

public class ExplorerTreeNodeFile extends ExplorerTreeNode {
    private String checksum;
    private String checksumType;
    private String mimeType;
    private BigInteger size;
    private Integer imageHeight;
    private Integer imageWidth;
    private String sourceXDimensionUnit;
    private Integer sourceXDimensionValue;
    private String sourceYDimensionUnit;
    private Integer sourceYDimensionValue;
    private String duration;
    private String description;
    private String fileName;
    private Integer daoFileFolderId;
    private Integer daoFileId;

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

    public Integer getDaoFileFolderId() {
        return daoFileFolderId;
    }

    public void setDaoFileFolderId(Integer daoFileFolderId) {
        this.daoFileFolderId = daoFileFolderId;
    }

    public Integer getDaoFileId() {
        return daoFileId;
    }

    public void setDaoFileId(Integer daoFileId) {
        this.daoFileId = daoFileId;
    }
}
