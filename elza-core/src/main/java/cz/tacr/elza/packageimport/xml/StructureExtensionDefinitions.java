package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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
