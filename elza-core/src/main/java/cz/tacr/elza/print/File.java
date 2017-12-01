package cz.tacr.elza.print;

import cz.tacr.elza.domain.DmsFile;

public class File {

    private Integer fileId;
    private String name;
    private String fileName;
    private Integer fileSize;
    private String mimeType;
    private Integer pagesCount;

    public File(DmsFile dmsFile) {
        this.fileId = dmsFile.getFileId();
        this.name = dmsFile.getName();
        this.fileName = dmsFile.getFileName();
        this.fileSize = dmsFile.getFileSize();
        this.mimeType = dmsFile.getMimeType();
        this.pagesCount = dmsFile.getPagesCount();
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(final Integer fileId) {
        this.fileId = fileId;
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

    public static File newInstance(DmsFile dmsFile) {
        return new File(dmsFile);
    }
}
