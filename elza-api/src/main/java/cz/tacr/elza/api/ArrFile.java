package cz.tacr.elza.api;

/**
 * Soubor ve Fund
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
public interface ArrFile<F extends ArrFund> extends DmsFile {

    F getFund();

    void setFund(F fund);
}
