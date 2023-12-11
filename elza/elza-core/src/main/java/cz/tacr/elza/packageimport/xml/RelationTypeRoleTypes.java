package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO TODO - seznam.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "relation-type-role-types")
@XmlType(name = "relation-type-role-types")
public class RelationTypeRoleTypes {

    @XmlElement(name = "relation-type-role-type", required = true)
    private List<RelationTypeRoleType> relationTypeRoleTypes;

    public List<RelationTypeRoleType> getRelationTypeRoleTypes() {
        return relationTypeRoleTypes;
    }

    public void setRelationTypeRoleTypes(final List<RelationTypeRoleType> relationTypeRoleTypes) {
        this.relationTypeRoleTypes = relationTypeRoleTypes;
    }
}
