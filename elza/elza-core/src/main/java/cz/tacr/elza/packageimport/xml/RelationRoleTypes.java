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
@XmlRootElement(name = "relation-role-types")
@XmlType(name = "relation-role-types")
public class RelationRoleTypes {

    @XmlElement(name = "relation-role-type", required = true)
    private List<RelationRoleType> relationRoleTypes;

    public List<RelationRoleType> getRelationRoleTypes() {
        return relationRoleTypes;
    }

    public void setRelationRoleTypes(final List<RelationRoleType> relationRoleTypes) {
        this.relationRoleTypes = relationRoleTypes;
    }
}
