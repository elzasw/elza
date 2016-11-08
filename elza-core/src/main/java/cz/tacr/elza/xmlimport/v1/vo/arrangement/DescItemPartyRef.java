package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;
import cz.tacr.elza.xmlimport.v1.vo.party.AbstractParty;

/**
 * Odkaz na osobu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "desc-item-party-ref", namespace = NamespaceInfo.NAMESPACE)
public class DescItemPartyRef extends AbstractDescItem {

    /** Odkaz do seznamu osob. */
    @XmlAttribute(name = "party-id", required = true)
    private String partyId;

    @XmlTransient
    private AbstractParty party;

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(final String partyId) {
        this.partyId = partyId;
    }

    public AbstractParty getParty() {
        return party;
    }

    public void setParty(final AbstractParty party) {
        this.party = party;
    }
}
