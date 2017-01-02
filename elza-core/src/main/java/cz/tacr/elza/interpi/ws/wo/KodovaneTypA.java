//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.24 at 04:42:06 PM CET 
//


package cz.tacr.elza.interpi.ws.wo;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for kodovaneTypA.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="kodovaneTypA"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="kód země (MARC21)"/&gt;
 *     &lt;enumeration value="kód země (ISO 3166)"/&gt;
 *     &lt;enumeration value="MDT"/&gt;
 *     &lt;enumeration value="skupina konspektu"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "kodovaneTypA")
@XmlEnum
public enum KodovaneTypA {

    @XmlEnumValue("k\u00f3d zem\u011b (MARC21)")
    KÓD_ZEMĚ_MARC_21("k\u00f3d zem\u011b (MARC21)"),
    @XmlEnumValue("k\u00f3d zem\u011b (ISO 3166)")
    KÓD_ZEMĚ_ISO_3166("k\u00f3d zem\u011b (ISO 3166)"),
    MDT("MDT"),
    @XmlEnumValue("skupina konspektu")
    SKUPINA_KONSPEKTU("skupina konspektu");
    private final String value;

    KodovaneTypA(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static KodovaneTypA fromValue(String v) {
        for (KodovaneTypA c: KodovaneTypA.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
