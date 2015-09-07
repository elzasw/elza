package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * hodnota atributu archivního popisu typu strojově zpracovatelná datace.
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataUnitdate extends Serializable{


    String getValue();


    void setValue(final String value);
}
