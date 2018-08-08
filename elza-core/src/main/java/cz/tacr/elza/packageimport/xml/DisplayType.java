package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Formát pro zobrazení typu Integer.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "display-type")
public enum DisplayType {

    /**
     * Obecné číslo.
     */
    NUMBER,

    /**
     * Čas ve formátu HH:mm:ss.
     */
    DURATION

}
