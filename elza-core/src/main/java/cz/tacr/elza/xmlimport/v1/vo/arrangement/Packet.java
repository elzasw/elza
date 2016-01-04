package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
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

    /** Kód typu obalu. */
    @XmlAttribute(name = "packet-type-code")
    private String packetTypeCode;

    /** Úložné číslo*/
    @XmlID
    @XmlAttribute(name = "storage-number")
    private String storageNumber;

    /** Neplatný/neaktivní */
    @XmlAttribute(name = "invalid")
    private boolean invalid;

    public String getPacketTypeCode() {
        return packetTypeCode;
    }

    public void setPacketTypeCode(String packetTypeCode) {
        this.packetTypeCode = packetTypeCode;
    }

    public String getStorageNumber() {
        return storageNumber;
    }

    public void setStorageNumber(String storageNumber) {
        this.storageNumber = storageNumber;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
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
