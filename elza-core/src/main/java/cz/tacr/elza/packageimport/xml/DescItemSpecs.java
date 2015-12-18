package cz.tacr.elza.packageimport.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO DescItemSpecs.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "desc-item-specs")
@XmlType(name = "desc-item-specs")
public class DescItemSpecs {

    @XmlElement(name = "desc-item-spec", required = true)
    private List<DescItemSpec> descItemSpecs;

    public List<DescItemSpec> getDescItemSpecs() {
        return descItemSpecs;
    }

    public void setDescItemSpecs(final List<DescItemSpec> descItemSpecs) {
        this.descItemSpecs = descItemSpecs;
    }
}
