package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

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
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ArrDataPacketRef extends ArrData {

    public static final String PACKET = "packet";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrPacket.class)
    @JoinColumn(name = "packetId", nullable = false)
    private ArrPacket packet;

    @Transient
    private final ArrDataPacketRefIndexProvider indexProvider;

    public ArrDataPacketRef(ArrDataPacketRefIndexProvider indexProvider) {
        this.indexProvider = indexProvider;
    }

    public ArrDataPacketRef() {
        this.indexProvider = new ArrDataPacketRefIndexProvider() {
            @Override
            public String getStorageNumber() {
                return getPacket().getStorageNumber();
            }

            @Override
            public RulPacketType getPacketType() {
                return getPacket().getPacketType();
            }
        };
    }

    public ArrPacket getPacket() {
        return packet;
    }

    public void setPacket(final ArrPacket packet) {
        this.packet = packet;
    }

    @Override
    @Field
    public Integer getSpecification() {
        return indexProvider.getSpecification();
    }

    @Override
    @Field
    public String getFulltextValue() {
        return indexProvider.getFulltextValue();
    }

    public static abstract class ArrDataPacketRefIndexProvider {

        public abstract String getStorageNumber();

        public abstract RulPacketType getPacketType();

        public Integer getSpecification() {
            RulPacketType packetType = getPacketType();
            if (packetType == null) {
                return null;
            }
            return packetType.getPacketTypeId();
        }

        public String getFulltextValue() {
            String fulltext = getStorageNumber();
            RulPacketType packetType = getPacketType();
            if (packetType != null) {
                return packetType.getName() + ": " + fulltext;
            }
            return fulltext;
        }
    }
}
