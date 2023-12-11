package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * VO StructuredType.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "structure-type")
public class StructureType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "validValueFromVersion", required = false)
    private String validValueFromVersion;

    @XmlAttribute(name = "anonymous", required = false)
    private Boolean anonymous = false;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getValidValueFromVersion() {
        return validValueFromVersion;
    }

    public void setValidValueFromVersion(String validValueFromVersion) {
        this.validValueFromVersion = validValueFromVersion;
    }

    public Boolean getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(final Boolean anonymous) {
        this.anonymous = anonymous;
    }
}
