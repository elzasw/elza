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
 * <p>Java class for titulTypA.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="titulTypA"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="tituly před jménem"/&gt;
 *     &lt;enumeration value="tituly za jménem"/&gt;
 *     &lt;enumeration value="šlechtické tituly"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "titulTypA")
@XmlEnum
public enum TitulTypA {

    @XmlEnumValue("tituly p\u0159ed jm\u00e9nem")
    TITULY_PŘED_JMÉNEM("tituly p\u0159ed jm\u00e9nem"),
    @XmlEnumValue("tituly za jm\u00e9nem")
    TITULY_ZA_JMÉNEM("tituly za jm\u00e9nem"),
    @XmlEnumValue("\u0161lechtick\u00e9 tituly")
    ŠLECHTICKÉ_TITULY("\u0161lechtick\u00e9 tituly");
    private final String value;

    TitulTypA(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TitulTypA fromValue(String v) {
        for (TitulTypA c: TitulTypA.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
