package cz.tacr.elza.suzap.v1.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

/**
 * Odkaz na osobu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "value-party-ref", namespace = NamespaceInfo.NAMESPACE)
public class ValuePartyRef extends AbstractValue {

    /** Odkaz do seznamu osob. */
    @XmlIDREF
    @XmlElement(required = true)
    private Party party;

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }
}
