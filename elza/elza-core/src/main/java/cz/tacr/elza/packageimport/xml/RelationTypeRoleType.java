package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO TODO.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "relation-type-role-type")
public class RelationTypeRoleType {

    @XmlAttribute(name = "relation-type", required = true)
    private String relationType;

    @XmlAttribute(name = "role-type", required = true)
    private String roleType;

    @XmlElement(name = "repeatable", required = true)
    private Boolean repeatable;

    @XmlElement(name = "view-order", required = true)
    private Integer viewOrder;

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(final String relationType) {
        this.relationType = relationType;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(final String roleType) {
        this.roleType = roleType;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }
}
