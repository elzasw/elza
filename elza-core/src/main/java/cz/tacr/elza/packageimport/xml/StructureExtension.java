package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * VO StructureExtension.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "structure-extension")
public class StructureExtension {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "structure-type", required = true)
    private String structureType;

    @XmlElement(name = "name", required = true)
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getStructureType() {
        return structureType;
    }

    public void setStructureType(final String structureType) {
        this.structureType = structureType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
