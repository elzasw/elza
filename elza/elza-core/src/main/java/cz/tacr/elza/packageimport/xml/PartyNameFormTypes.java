package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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
