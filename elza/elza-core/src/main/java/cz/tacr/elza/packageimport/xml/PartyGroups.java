package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO skupina osoby - seznam.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "party-groups")
@XmlType(name = "party-groups")
public class PartyGroups {

    @XmlElement(name = "party-group", required = true)
    private List<PartyGroup> partyGroups;

    public List<PartyGroup> getPartyGroups() {
        return partyGroups;
    }

    public void setPartyGroups(final List<PartyGroup> partyGroups) {
        this.partyGroups = partyGroups;
    }
}
