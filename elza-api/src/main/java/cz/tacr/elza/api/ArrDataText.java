package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * hodnota atributu archivního popisu typu "neomezený" textový řetězec.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface ArrDataText extends Serializable{

    String getValue();

    void setValue(final String value);
}
