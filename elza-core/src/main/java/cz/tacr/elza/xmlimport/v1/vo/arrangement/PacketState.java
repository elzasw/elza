package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Stav obalu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 11. 2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "packet-type", namespace = NamespaceInfo.NAMESPACE)
@XmlEnum
public enum PacketState {
    OPEN,
    CLOSED,
    CANCELED;
}
