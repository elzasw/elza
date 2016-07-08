package cz.tacr.elza.api;

/**
 * Api Objekt Arr data file ref
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
public interface ArrDataFileRef<F extends ArrFile> {

    F getFile();

    void setFile(F file);
}
