package cz.tacr.elza.print;

import cz.tacr.elza.domain.DmsFile;

public class File {

    private final Integer fileId;

    private final String name;

    private final String fileName;

    private final Integer fileSize;

    private final String mimeType;

    private final Integer pagesCount;

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

    public String getFileName() {
        return fileName;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getName() {
        return name;
    }

    public Integer getPagesCount() {
        return pagesCount;
    }
}
