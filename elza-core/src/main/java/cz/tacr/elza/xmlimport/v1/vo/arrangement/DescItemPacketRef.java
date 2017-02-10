package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
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
    @XmlAttribute(name = "packet-id", required = true)
    private String packetId;

    @XmlTransient
    private Packet packet;

    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(final String packetId) {
        this.packetId = packetId;
    }

    public Packet getPacket() {
        return packet;
    }

    public void setPacket(final Packet packet) {
        this.packet = packet;
        if (packet != null) {
            packetId = packet.getPacketId();
        }
    }
}
