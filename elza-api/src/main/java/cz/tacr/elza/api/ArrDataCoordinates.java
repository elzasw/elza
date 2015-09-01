package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataCoordinates extends Serializable{


    String getValue();


    void setValue(final String value);
}
