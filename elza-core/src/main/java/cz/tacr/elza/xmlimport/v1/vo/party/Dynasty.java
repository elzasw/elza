package cz.tacr.elza.xmlimport.v1.vo.party;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.utils.PartyType;
import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Rod.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dynasty", namespace = NamespaceInfo.NAMESPACE)
public class Dynasty extends AbstractParty {

    /** Genealogie. */
    @XmlElement(name = "genealogy", required = true)
    private String genealogy;

    public Dynasty() {
        super(PartyType.DYNASTY);
    }

    public String getGenealogy() {
        return genealogy;
    }

    public void setGenealogy(final String genealogy) {
        this.genealogy = genealogy;
    }
}
