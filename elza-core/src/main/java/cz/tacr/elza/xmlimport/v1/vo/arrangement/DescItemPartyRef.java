package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
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
    @XmlIDREF
    @XmlAttribute(name = "party-id", required = true)
    private AbstractParty party;

    public AbstractParty getParty() {
        return party;
    }

    public void setParty(AbstractParty party) {
        this.party = party;
    }
}
