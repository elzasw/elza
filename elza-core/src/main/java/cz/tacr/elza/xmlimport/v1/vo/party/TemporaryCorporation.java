package cz.tacr.elza.xmlimport.v1.vo.party;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Dočasná korporace.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tmp-corporation", namespace = NamespaceInfo.NAMESPACE)
public class TemporaryCorporation extends AbstractParty {

}
