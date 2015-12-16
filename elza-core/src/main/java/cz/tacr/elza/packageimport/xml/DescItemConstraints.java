package cz.tacr.elza.packageimport.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO DescItemConstraints.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "desc-item-constraints")
@XmlType(name = "desc-item-constraints")
public class DescItemConstraints {

    @XmlElement(name = "desc-item-constraint", required = true)
    private List<DescItemConstraint> descItemConstraints;

    public List<DescItemConstraint> getDescItemConstraints() {
        return descItemConstraints;
    }

    public void setDescItemConstraints(final List<DescItemConstraint> descItemConstraints) {
        this.descItemConstraints = descItemConstraints;
    }
}
