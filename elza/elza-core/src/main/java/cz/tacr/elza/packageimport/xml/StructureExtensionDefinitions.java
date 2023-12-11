package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO StructureExtensionDefinitions.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "structure-extension-definitions")
@XmlType(name = "structure-extension-definitions")
public class StructureExtensionDefinitions {

    @XmlElement(name = "structure-extension-definition", required = true)
    private List<StructureExtensionDefinition> structureExtensionDefinitions;

    public List<StructureExtensionDefinition> getStructureExtensions() {
        return structureExtensionDefinitions;
    }

    public void setStructureExtensions(final List<StructureExtensionDefinition> structureExtensionDefinitions) {
        this.structureExtensionDefinitions = structureExtensionDefinitions;
    }
}
