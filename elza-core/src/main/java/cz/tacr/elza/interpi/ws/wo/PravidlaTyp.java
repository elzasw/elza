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
 * <p>Java class for pravidlaTyp.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="pravidlaTyp"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="INTERPI"/&gt;
 *     &lt;enumeration value="AACR2"/&gt;
 *     &lt;enumeration value="RDA"/&gt;
 *     &lt;enumeration value="ZP"/&gt;
 *     &lt;enumeration value="EUROVOC"/&gt;
 *     &lt;enumeration value="PSH"/&gt;
 *     &lt;enumeration value="ČTT"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "pravidlaTyp")
@XmlEnum
public enum PravidlaTyp {

    INTERPI("INTERPI"),
    @XmlEnumValue("AACR2")
    AACR_2("AACR2"),
    RDA("RDA"),
    ZP("ZP"),
    EUROVOC("EUROVOC"),
    PSH("PSH"),
    ČTT("\u010cTT");
    private final String value;

    PravidlaTyp(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static PravidlaTyp fromValue(String v) {
        for (PravidlaTyp c: PravidlaTyp.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
