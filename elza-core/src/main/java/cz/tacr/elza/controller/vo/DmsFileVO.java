package cz.tacr.elza.controller.vo;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.domain.DmsFile;

/**
 * 
 * @since 13.3.2016
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class DmsFileVO {
    private Integer id;

    /**
     * Textový obsah souboru, je plněn při ukládání z klienta na server, pokud je editován na klientovi nebo je plněn při čtení, pokud je o to explicitně požádáno.
     */
    private String content;

    private String name;

    private String fileName;

    private Integer fileSize;

    private String mimeType;

    private String pagesCount;

    /**
     * Používá se při uploadu souboru na server
     */
    private MultipartFile file;

    public DmsFileVO() {

    }

    protected DmsFileVO(DmsFile srcFile) {
        id = srcFile.getFileId();
        fileName = srcFile.getFileName();
        mimeType = srcFile.getMimeType();
        name = srcFile.getName();
        fileSize = srcFile.getFileSize();
        if (srcFile.getPagesCount() != null) {
            pagesCount = srcFile.getPagesCount().toString();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getPagesCount() {
        return pagesCount;
    }

    public void setPagesCount(String pagesCount) {
        this.pagesCount = pagesCount;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    static public DmsFileVO newInstance(DmsFile srcFile) {
        DmsFileVO result = new DmsFileVO(srcFile);
        return result;
    }

    public DmsFile createEntity() {
        DmsFile result = new DmsFile();
        copyTo(result);
        return result;
    }

    /**
     * Copy values from VO to DmsFile
     * 
     * @param result
     */
    protected void copyTo(DmsFile target) {
        target.setFileId(id);
        target.setFileName(fileName);
        target.setFileSize(fileSize);
        target.setMimeType(mimeType);
        target.setName(name);
        if (pagesCount != null) {
            int pageCnt = Integer.parseInt(pagesCount);
            target.setPagesCount(pageCnt);
        }

    }

}
