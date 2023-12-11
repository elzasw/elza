package cz.tacr.elza.packageimport.xml;

import cz.tacr.elza.domain.RulStructureDefinition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * VO StructureDefinition.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "structure-definition")
public class StructureDefinition {

    @XmlAttribute(name = "structure-type", required = true)
    private String structureType;

    @XmlAttribute(name = "filename", required = true)
    private String filename;

    @XmlElement(name = "def-type", required = true)
    private RulStructureDefinition.DefType defType;

    @XmlElement(name = "priority", required = true)
    private Integer priority;

    @XmlAttribute(name="compatibility-rul-package")
    private Integer compatibilityRulPackage;

    public String getStructureType() {
        return structureType;
    }

    public void setStructureType(final String structureType) {
        this.structureType = structureType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public RulStructureDefinition.DefType getDefType() {
        return defType;
    }

    public void setDefType(final RulStructureDefinition.DefType defType) {
        this.defType = defType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public Integer getCompatibilityRulPackage() {
        return compatibilityRulPackage;
    }

    public void setCompatibilityRulPackage(Integer compatibilityRulPackage) {
        this.compatibilityRulPackage = compatibilityRulPackage;
    }
}
