package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO vztah typu třídy - seznam.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "relation-class-types")
@XmlType(name = "relation-class-types")
public class RelationClassTypes {

    @XmlElement(name = "relation-class-type", required = true)
    private List<RelationClassType> relationClassTypes;

    public List<RelationClassType> getRelationClassTypes() {
        return relationClassTypes;
    }

    public void setRelationClassTypes(final List<RelationClassType> relationClassTypes) {
        this.relationClassTypes = relationClassTypes;
    }
}
