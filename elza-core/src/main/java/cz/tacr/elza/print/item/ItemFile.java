package cz.tacr.elza.print.item;


import java.io.File;

import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.DmsFile;

/**
 * Link to the file
 */
public class ItemFile extends AbstractItem {

    private Integer fileId;
    private String name;
    private String fileName;
    private Integer fileSize;
    private String mimeType;
    private Integer pagesCount;
    DmsFile value;

    public ItemFile(final ArrFile value) {
        super();
        this.value = value;
    }

    @Override
    public Object getValue() {
    	return value;
    }
    
    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(final Integer fileId) {
        this.fileId = fileId;
    }

    @Override
    public String serializeValue() {
        return getName() + " (" + getFileName() + ")";
    }

    public File getFile() {
        return getValue(File.class);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(final Integer fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getPagesCount() {
        return pagesCount;
    }

    public void setPagesCount(final Integer pagesCount) {
        this.pagesCount = pagesCount;
    }
}
