package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 27.6.16
 */
public interface ArrItemFileRef<F extends ArrFile> extends ArrItemData {

    F getFile();


    void setFile(F file);
}
