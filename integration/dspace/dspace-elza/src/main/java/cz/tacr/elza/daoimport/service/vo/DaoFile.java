package cz.tacr.elza.daoimport.service.vo;

import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

import org.dspace.content.DCDate;

import cz.tacr.elza.metadataconstants.MetadataEnum;

public class DaoFile {

    private Path file;
    private String createdDate;
    private String description;
    private Map<MetadataEnum, String> techMD;

    public Path getFile() {
        return file;
    }

    public void setFile(Path contentFile) {
        this.file = contentFile;
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
