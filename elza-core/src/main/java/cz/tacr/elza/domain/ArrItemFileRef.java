package cz.tacr.elza.domain;

import java.util.Objects;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 27.6.16
 */
public class ArrItemFileRef extends ArrItemData {

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFile.class)
    @JoinColumn(name = "fileId", nullable = false)
    private ArrFile file;

    public ArrFile getFile() {
        return file;
    }

    public void setFile(final ArrFile packet) {
        this.file = packet;
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
