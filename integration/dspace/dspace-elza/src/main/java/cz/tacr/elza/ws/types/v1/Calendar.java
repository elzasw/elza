
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for calendar.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="calendar"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *     &lt;enumeration value="gregorian"/&gt;
 *     &lt;enumeration value="julian"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "calendar")
@XmlEnum
public enum Calendar {

    @XmlEnumValue("gregorian")
    GREGORIAN("gregorian"),
    @XmlEnumValue("julian")
    JULIAN("julian");
    private final String value;

    Calendar(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Calendar fromValue(String v) {
        for (Calendar c: Calendar.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
