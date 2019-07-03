
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for checksumType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="checksumType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *     &lt;enumeration value="MD5"/&gt;
 *     &lt;enumeration value="SHA-1"/&gt;
 *     &lt;enumeration value="SHA-256"/&gt;
 *     &lt;enumeration value="SHA-384"/&gt;
 *     &lt;enumeration value="SHA-512"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "checksumType")
@XmlEnum
public enum ChecksumType {

    @XmlEnumValue("MD5")
    MD_5("MD5"),
    @XmlEnumValue("SHA-1")
    SHA_1("SHA-1"),
    @XmlEnumValue("SHA-256")
    SHA_256("SHA-256"),
    @XmlEnumValue("SHA-384")
    SHA_384("SHA-384"),
    @XmlEnumValue("SHA-512")
    SHA_512("SHA-512");
    private final String value;

    ChecksumType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ChecksumType fromValue(String v) {
        for (ChecksumType c: ChecksumType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
