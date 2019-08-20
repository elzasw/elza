package cz.tacr.elza.daoimport.service.vo;

import java.nio.file.Path;

public class MetadataInfo {
    private String mimeType;
    private String checkSum;

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(String checkSum) {
        this.checkSum = checkSum;
    }
}
