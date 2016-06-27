package cz.tacr.elza.domain;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 27.6.16
 */
public class ArrItemFileRef extends ArrItemData implements cz.tacr.elza.api.ArrItemFileRef<ArrFile> {

    private ArrFile file;

    @Override
    public ArrFile getFile() {
        return file;
    }

    @Override
    public void setFile(ArrFile packet) {
        this.file = packet;
    }

    @Override
    public String toString() {
        return (file != null ) ? file.getName() : null;
    }
}
