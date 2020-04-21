package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO PartTypes.
 *
 * @since 21.04.2020
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "part-types")
@XmlType(name = "part-types")
public class PartTypes {

    @XmlElement(name = "part-type", required = true)
    private List<PartType> partTypes;

    public List<PartType> getPartTypes() {
        return partTypes;
    }

    public void setPartTypes(final List<PartType> partTypes) {
        this.partTypes = partTypes;
    }
}
