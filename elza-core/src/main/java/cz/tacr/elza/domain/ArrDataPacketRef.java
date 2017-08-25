package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;



/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_packet_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataPacketRef extends ArrData {

    public static final String PACKET = "packet";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrPacket.class)
    @JoinColumn(name = "packetId", nullable = false)
    private ArrPacket packet;

    @Column(name = "packetId", updatable = false, insertable = false)
    private Integer packetId;

    @Field
    public Integer getSpecification() {
        RulPacketType packetType = packet.getPacketType();

        return packetType == null ? null : packetType.getPacketTypeId();
    }

    public ArrPacket getPacket() {
        return packet;
    }

    public void setPacket(final ArrPacket packet) {
        this.packet = packet;
        this.packetId = packet == null ? null : packet.getPacketId();
    }

    public Integer getPacketId() {
        return packetId;
    }

    @Override
    public String getFulltextValue() {
        RulPacketType packetType = packet.getPacketType();
        String fulltext;
        if (packetType == null) {
            fulltext = packet.getStorageNumber();
        } else {
            fulltext = packetType.getName() + ": " + packet.getStorageNumber();
        }
        return fulltext;
    }
}
