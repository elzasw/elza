package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO StructureTypes.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "structure-types")
@XmlType(name = "structure-types")
public class StructureTypes {

    @XmlElement(name = "structure-type", required = true)
    private List<StructureType> structureTypes;

    public List<StructureType> getStructureTypes() {
        return structureTypes;
    }

    public void setStructureTypes(final List<StructureType> structureTypes) {
        this.structureTypes = structureTypes;
    }
}
