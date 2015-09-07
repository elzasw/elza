package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * Hodnota atributu archivního popisu typu Integer.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface ArrDataInteger extends Serializable{


    Integer getValue();


    void setValue(final Integer value);
}
