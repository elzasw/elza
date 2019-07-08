package cz.tacr.elza.daoimport.service.vo;

import java.nio.file.Path;

public class DaoFile {

    private Path contentFile;
    private Path metadataFile;
    private Path thumbnailFile;

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

    public void setThumbnailFile(Path thumbnailFile) {
        this.thumbnailFile = thumbnailFile;
    }
}
