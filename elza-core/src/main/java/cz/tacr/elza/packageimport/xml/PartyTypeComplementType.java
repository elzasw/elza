package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO vazba M:N mezi typem osoby a typem doplňku jména.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "party-type-complement-type")
public class PartyTypeComplementType {

    @XmlAttribute(name = "party-type", required = true)
    private String partyType;

    @XmlAttribute(name = "complement-type", required = true)
    private String complementType;

    @XmlElement(name = "repeatable", required = true)
    private Boolean repeatable;

    public String getComplementType() {
        return complementType;
    }

    public void setComplementType(final String complementType) {
        this.complementType = complementType;
    }

    public String getPartyType() {
        return partyType;
    }

    public void setPartyType(final String partyType) {
        this.partyType = partyType;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }
}
