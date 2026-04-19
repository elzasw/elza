package cz.tacr.elza.config.export;

public enum JasperFormat {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    RTF("rtf", "application/rtf"),
    ODT("odt", "application/vnd.oasis.opendocument.text");

    private final String extension;
    private final String mimeType;

    JasperFormat(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }
}
