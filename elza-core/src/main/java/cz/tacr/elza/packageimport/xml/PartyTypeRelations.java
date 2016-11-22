package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO typ vztahu osoby - seznam.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "party-type-relations")
@XmlType(name = "party-type-relations")
public class PartyTypeRelations {

    @XmlElement(name = "party-type-relation", required = true)
    private List<PartyTypeRelation> partyTypeRelations;

    public List<PartyTypeRelation> getPartyTypeRelations() {
        return partyTypeRelations;
    }

    public void setPartyTypeRelations(final List<PartyTypeRelation> partyTypeRelations) {
        this.partyTypeRelations = partyTypeRelations;
    }
}
