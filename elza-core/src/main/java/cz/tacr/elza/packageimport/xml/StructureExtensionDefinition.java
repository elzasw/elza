package cz.tacr.elza.packageimport.xml;

import cz.tacr.elza.domain.RulStructureExtensionDefinition;

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
@XmlType(name = "structure-extension-definition")
public class StructureExtensionDefinition {

    @XmlAttribute(name = "structure-extension", required = true)
    private String structureExtension;

    @XmlAttribute(name = "filename", required = true)
    private String filename;

    @XmlElement(name = "def-type", required = true)
    private RulStructureExtensionDefinition.DefType defType;

    @XmlElement(name = "priority", required = true)
    private Integer priority;

    @XmlAttribute(name="compatibility-rul-package")
    private Integer compatibilityRulPackage;

    public String getStructureExtension() {
        return structureExtension;
    }

    public void setStructureExtension(final String structureExtension) {
        this.structureExtension = structureExtension;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public RulStructureExtensionDefinition.DefType getDefType() {
        return defType;
    }

    public void setDefType(final RulStructureExtensionDefinition.DefType defType) {
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
