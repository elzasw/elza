package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * VO PartType.
 *
 * @since 21.04.2020
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "part-type")
public class PartType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "parent_part")
    private Boolean parentPart = false;

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

    public Boolean getParentPart() {
        return parentPart;
    }

    public void setParentPart(Boolean parentPart) {
        this.parentPart = parentPart;
    }
}
