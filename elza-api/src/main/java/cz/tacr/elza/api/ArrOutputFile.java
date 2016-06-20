package cz.tacr.elza.api;

/**
 * Soubor v Output
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
public interface ArrOutputFile<R extends ArrOutputResult> extends DmsFile {

    R getOutputResult();

    void setOutputResult(R outputResult);
}
