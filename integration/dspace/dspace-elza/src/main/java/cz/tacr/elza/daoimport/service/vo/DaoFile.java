package cz.tacr.elza.daoimport.service.vo;

import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

import org.dspace.content.DCDate;

import cz.tacr.elza.metadataconstants.MetadataEnum;

public class DaoFile {

    private Path contentFile;
    private Path metadataFile;
    private Path thumbnailFile;
    private String createdDate;
    private Integer orderNumber;
    private String description;
    private Map<MetadataEnum, String> techMD;

    public Path getContentFile() {
        return contentFile;
    }

    public void setContentFile(Path contentFile) {
        this.contentFile = contentFile;
    }

    public Path getMetadataFile() {
        return metadataFile;
    }

    public void setMetadataFile(Path metadataFile) {
        this.metadataFile = metadataFile;
    }

    public Path getThumbnailFile() {
        return thumbnailFile;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        String date = null;
        if (createdDate != null) {
            DCDate dcDate = new DCDate(createdDate);
            date = dcDate.toString();
        }
        this.createdDate = date;
    }

    public void setThumbnailFile(Path thumbnailFile) {
        this.thumbnailFile = thumbnailFile;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public Map<MetadataEnum, String> getTechMD() {
        return techMD;
    }

    public void setTechMD(Map<MetadataEnum, String> techMD) {
        this.techMD = techMD;
    }
}
