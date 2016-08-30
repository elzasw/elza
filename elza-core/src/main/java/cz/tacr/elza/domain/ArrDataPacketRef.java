package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;



/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_packet_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataPacketRef extends ArrData implements cz.tacr.elza.api.ArrDataPacketRef<ArrPacket> {

    public static final String PACKET = "packet";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrPacket.class)
    @JoinColumn(name = "packetId", nullable = false)
    private ArrPacket packet;

    @Override
    @Field
    public Integer getSpecification() {
        return packet.getPacketType().getPacketTypeId();
    }

    @Override
    public ArrPacket getPacket() {
        return packet;
    }

    @Override
    public void setPacket(final ArrPacket packet) {
        this.packet = packet;
    }

    @Override
    public String getFulltextValue() {
        return packet.getPacketType().getName() + ": " + packet.getStorageNumber();
    }
}
