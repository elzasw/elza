package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO StructureDefinitions.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "structure-definitions")
@XmlType(name = "structure-definitions")
public class StructureDefinitions {

    @XmlElement(name = "structure-definition", required = true)
    private List<StructureDefinition> structureDefinitions;

    public List<StructureDefinition> getStructureDefinitions() {
        return structureDefinitions;
    }

    public void setStructureDefinitions(final List<StructureDefinition> structureDefinitions) {
        this.structureDefinitions = structureDefinitions;
    }
}
