package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO typ formy jména - seznam.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "party-name-form-types")
@XmlType(name = "party-name-form-types")
public class PartyNameFormTypes {

    @XmlElement(name = "party-name-form-type", required = true)
    private List<PartyNameFormType> partyNameFormTypes;

    public List<PartyNameFormType> getPartyNameFormTypes() {
        return partyNameFormTypes;
    }

    public void setPartyNameFormTypes(final List<PartyNameFormType> partyNameFormTypes) {
        this.partyNameFormTypes = partyNameFormTypes;
    }
}
