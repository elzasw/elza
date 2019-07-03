
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for certainity.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="certainity"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *     &lt;enumeration value="approximate"/&gt;
 *     &lt;enumeration value="approximate-notafter"/&gt;
 *     &lt;enumeration value="approximate-notbefore"/&gt;
 *     &lt;enumeration value="exact"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "certainity")
@XmlEnum
public enum Certainity {

    @XmlEnumValue("approximate")
    APPROXIMATE("approximate"),
    @XmlEnumValue("approximate-notafter")
    APPROXIMATE_NOTAFTER("approximate-notafter"),
    @XmlEnumValue("approximate-notbefore")
    APPROXIMATE_NOTBEFORE("approximate-notbefore"),
    @XmlEnumValue("exact")
    EXACT("exact");
    private final String value;

    Certainity(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Certainity fromValue(String v) {
        for (Certainity c: Certainity.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
