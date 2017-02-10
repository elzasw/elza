package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Obal.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 12. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "packet", namespace = NamespaceInfo.NAMESPACE)
public class Packet {

    /** Id obalu. */
    @XmlAttribute(name = "packet-id", required = true)
    private String packetId;

    /** Kód typu obalu. */
    @XmlAttribute(name = "packet-type-code")
    private String packetTypeCode;

    /** Úložné číslo*/
    @XmlAttribute(name = "storage-number", required = true)
    private String storageNumber;

    /** Stav */
    @XmlAttribute(name = "state", required = true)
    private PacketState state;

    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(final String packetId) {
        this.packetId = packetId;
    }

    public String getPacketTypeCode() {
        return packetTypeCode;
    }

    public void setPacketTypeCode(final String packetTypeCode) {
        this.packetTypeCode = packetTypeCode;
    }

    public String getStorageNumber() {
        return storageNumber;
    }

    public void setStorageNumber(final String storageNumber) {
        this.storageNumber = storageNumber;
    }

    public PacketState getState() {
        return state;
    }

    public void setState(final PacketState state) {
        this.state = state;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
