package cz.tacr.elza.domain;

import java.util.Objects;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 27.6.16
 */
@Deprecated
public class ArrItemFileRef extends ArrItemData {

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFile.class)
    @JoinColumn(name = "fileId", nullable = false)
    private ArrFile file;

    private Integer fileId;

    public ArrFile getFile() {
        return file;
    }

    public void setFile(final ArrFile file) {
        this.file = file;
        this.fileId = file == null ? null : file.getFileId();
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(final Integer fileId) {
        this.fileId = fileId;
    }

    @Override
    public String toString() {
        return (file != null ) ? file.getName() : null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemFileRef that = (ArrItemFileRef) o;
        return Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), file);
    }
}
