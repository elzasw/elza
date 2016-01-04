package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Odkaz na obal.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 12. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "desc-item-packet-ref", namespace = NamespaceInfo.NAMESPACE)
public class DescItemPacketRef extends AbstractDescItem {

    /** Odkaz do seznamu obalů. */
    @XmlIDREF
    @XmlAttribute(name = "packet-id", required = true)
    private Packet packet;

    public Packet getPacket() {
        return packet;
    }

    public void setPacket(Packet packet) {
        this.packet = packet;
    }
}
