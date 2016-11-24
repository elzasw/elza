package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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
