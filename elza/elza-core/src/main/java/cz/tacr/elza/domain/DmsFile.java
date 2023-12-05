package cz.tacr.elza.domain;

import java.io.File;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Dms Soubor.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
@Table
@Entity(name = "dms_file")
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class DmsFile {

    public final static String FIELD_FILE_ID = "fileId";

    public static final String FIELD_NAME = "name";

    public static final String FIELD_FILE_NAME = "fileName";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
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

    /**
     * @return id souboru
     */
    public Integer getFileId() {
        return fileId;
    }

    /**
     * Nastaví id souboru
     * @param fileId id souboru
     */
    public void setFileId(final Integer fileId) {
        this.fileId = fileId;
    }

    /**
     * @return název souboru
     */
    public String getName() {
        return name;
    }

    /**
     * @param name název souboru
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return Reálný název souboru
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName Reálný název souboru
     */
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return velikost
     */
    public Integer getFileSize() {
        return fileSize;
    }

    /**
     * @param fileSize velikost
     */
    public void setFileSize(final Integer fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * @return mime
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType mime
     */
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return Počet stran (pouze u pdf)
     */
    public Integer getPagesCount() {
        return pagesCount;
    }

    /**
     * @param pagesCount Počet stran (pouze u pdf)
     */
    public void setPagesCount(final Integer pagesCount) {
        this.pagesCount = pagesCount;
    }

    @Override
    public String toString() {
        return "DmsFile pk=" + fileId;
    }

    public File getFile() {
        return file;
    }

    public void setFile(final File file) {
        this.file = file;
    }
}
