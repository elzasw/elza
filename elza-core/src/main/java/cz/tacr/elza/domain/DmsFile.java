package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.File;

/**
 * Dms Soubor
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
@Entity(name = "dms_file")
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class DmsFile implements cz.tacr.elza.api.DmsFile {

    public static final String NAME = "name";
    public static final String FILE_NAME = "file_name";
    @Id
    @GeneratedValue
    private Integer fileId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private Integer fileSize;

    @Column(nullable = false)
    private String mimeType;

    @Column
    private Integer pagesCount;

    @Transient
    private File file;

    @Override
    public Integer getFileId() {
        return fileId;
    }

    @Override
    public void setFileId(Integer fileId) {
        this.fileId = fileId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public Integer getFileSize() {
        return fileSize;
    }

    @Override
    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public Integer getPagesCount() {
        return pagesCount;
    }

    @Override
    public void setPagesCount(Integer pagesCount) {
        this.pagesCount = pagesCount;
    }

    @Override
    public String toString() {
        return "DmsFile pk=" + fileId;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
