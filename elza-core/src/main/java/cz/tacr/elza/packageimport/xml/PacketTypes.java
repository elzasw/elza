package cz.tacr.elza.packageimport.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO PacketTypes.
 *
 * @author Martin Šlapa
 * @since 21.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "packet-types")
@XmlType(name = "packet-types")
public class PacketTypes {

    @XmlElement(name = "packet-type", required = true)
    private List<PacketType> packetTypes;

    public List<PacketType> getPacketTypes() {
        return packetTypes;
    }

    public void setPacketTypes(final List<PacketType> packetTypes) {
        this.packetTypes = packetTypes;
    }
}
