package cz.tacr.elza.xmlimport.v1.vo.party;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.utils.PartyType;
import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Osoba.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 10. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "person", namespace = NamespaceInfo.NAMESPACE)
public class Person extends AbstractParty {

    public Person() {
        super(PartyType.PERSON);
    }
}
