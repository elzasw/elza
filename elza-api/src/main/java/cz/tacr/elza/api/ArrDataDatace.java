package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * Hodnota atributu archivního popisu typu Datace.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface ArrDataDatace extends Serializable{


    String getValue();


    void setValue(final String value);
}
