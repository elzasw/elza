package cz.tacr.elza.packageimport.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO DescItemTypes.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "desc-item-types")
@XmlType(name = "desc-item-types")
public class DescItemTypes {

    @XmlElement(name = "desc-item-type", required = true)
    private List<DescItemType> descItemTypes;

    public List<DescItemType> getDescItemTypes() {
        return descItemTypes;
    }

    public void setDescItemTypes(final List<DescItemType> descItemTypes) {
        this.descItemTypes = descItemTypes;
    }
}
