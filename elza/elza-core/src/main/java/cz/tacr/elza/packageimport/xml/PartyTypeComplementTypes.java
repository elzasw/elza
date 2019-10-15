package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO vazba M:N mezi typem osoby a typem doplňku jména - seznam.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "party-type-complement-types")
@XmlType(name = "party-type-complement-types")
public class PartyTypeComplementTypes {

    @XmlElement(name = "party-type-complement-type", required = true)
    private List<PartyTypeComplementType> partyTypeComplementTypes;

    public List<PartyTypeComplementType> getPartyTypeComplementTypes() {
        return partyTypeComplementTypes;
    }

    public void setPartyTypeComplementTypes(final List<PartyTypeComplementType> partyTypeComplementTypes) {
        this.partyTypeComplementTypes = partyTypeComplementTypes;
    }
}
