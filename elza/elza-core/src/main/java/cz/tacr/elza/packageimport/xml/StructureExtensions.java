package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO StructureExtensions.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "structure-extensions")
@XmlType(name = "structure-extensions")
public class StructureExtensions {

    @XmlElement(name = "structure-extension", required = true)
    private List<StructureExtension> structureExtensions;

    public List<StructureExtension> getStructureExtensions() {
        return structureExtensions;
    }

    public void setStructureExtensions(final List<StructureExtension> structureExtensions) {
        this.structureExtensions = structureExtensions;
    }
}
