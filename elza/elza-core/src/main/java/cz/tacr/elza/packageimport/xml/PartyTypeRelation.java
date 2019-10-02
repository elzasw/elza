package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO typ vztahu osoby.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "party-type-relation")
public class PartyTypeRelation {

    @XmlAttribute(name = "relation-type", required = true)
    private String relationType;

    @XmlAttribute(name = "party-type", required = true)
    private String partyType;

    @XmlElement(name = "name", required = true)
    private String name;

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

    public String getPartyType() {
        return partyType;
    }

    public void setPartyType(final String partyType) {
        this.partyType = partyType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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
