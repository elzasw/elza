package cz.tacr.elza.xmlimport.v1.vo;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.arrangement.Fund;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.Packet;
import cz.tacr.elza.xmlimport.v1.vo.party.AbstractParty;
import cz.tacr.elza.xmlimport.v1.vo.party.Dynasty;
import cz.tacr.elza.xmlimport.v1.vo.party.Event;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyGroup;
import cz.tacr.elza.xmlimport.v1.vo.party.Person;
import cz.tacr.elza.xmlimport.v1.vo.record.Record;

/**
 * Kořenový prvek entit pro import archivní pomůcky.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "xml-import", namespace = NamespaceInfo.NAMESPACE)
@XmlRootElement(name = "xml-import", namespace = NamespaceInfo.NAMESPACE)
public class XmlImport {

    /** Archivní pomůcka. */
    @XmlElement(name = "fund")
    private Fund fund;

    /** Seznam rejstříkových hesel. */
    @XmlElement(name = "record")
    @XmlElementWrapper(name = "record-list")
    private List<Record> records;

    /** Seznam osob. */
    @XmlElementWrapper(name = "party-list")
    @XmlElements(value = {
            @XmlElement(name = "person", type = Person.class),
            @XmlElement(name = "dynasty", type = Dynasty.class),
            @XmlElement(name = "party-group", type = PartyGroup.class),
            @XmlElement(name = "event", type = Event.class)
    })
    private List<AbstractParty> parties;

    /** Seznam obalů. */
    @XmlElement(name = "packet")
    @XmlElementWrapper(name = "packet-list")
    private List<Packet> packets;

    public Fund getFund() {
        return fund;
    }

    public void setFund(Fund fund) {
        this.fund = fund;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }

    public List<AbstractParty> getParties() {
        return parties;
    }

    public void setParties(List<AbstractParty> parties) {
        this.parties = parties;
    }

    public List<Packet> getPackets() {
        return packets;
    }

    public void setPackets(List<Packet> packets) {
        this.packets = packets;
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
