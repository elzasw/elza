package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * hodnota atributu archivního popisu typu referenční označení.
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataUnitid extends Serializable{


    String getValue();


    void setValue(final String value);
}
