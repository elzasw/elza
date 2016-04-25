package cz.tacr.elza.api;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Souřadnice
 *
 * @author Petr Compel
 * @since 20.4.2016
 */
public interface RegCoordinates<RR extends RegRecord> {

    /**
     *  @return souřadnice
     */
    Geometry getValue();

    /**
     * @param value souřadnice
     */
    void setValue(final Geometry value);

    /**
     * @return popis
     */
    String getDescription();


    /**
     * @param description popis
     */
    void setDescription(String description);

    /**
     * Vazba na heslo rejstříku.
     * @return  objekt hesla
     */
    RR getRegRecord();

    /**
     * Vazba na heslo rejstříku.
     * @param regRecord objekt hesla
     */
    void setRegRecord(RR regRecord);
}
