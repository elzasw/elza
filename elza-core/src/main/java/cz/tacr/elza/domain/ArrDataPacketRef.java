package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "arr_data_packet_ref")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ArrDataPacketRef extends ArrData {

    public static final String PACKET = "packet";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrPacket.class)
    @JoinColumn(name = "packetId", nullable = false)
    private ArrPacket packet;

    @Column(name = "packetId", updatable = false, insertable = false)
    private Integer packetId;

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
        this.packetId = packet == null ? null : packet.getPacketId();
    }

    public Integer getPacketId() {
        return packetId;
    }

    @JsonIgnore
    @Field
    public Integer getSpecification() {
        return indexProvider.getSpecification();
    }

    @Override
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

    @Override
    public ArrData copy() {
        ArrDataPacketRef data = new ArrDataPacketRef();
        data.setDataType(this.getDataType());
        data.setPacket(this.getPacket());
        return data;
    }

    @Override
    public void merge(final ArrData data) {
        this.setPacket(((ArrDataPacketRef) data).getPacket());
    }
}

