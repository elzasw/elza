package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Číselník obalů.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_packet")
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"fundId"}),
        @UniqueConstraint(columnNames = {"storageNumber"})})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrPacket implements cz.tacr.elza.api.ArrPacket<RulPacketType, ArrFund> {
    public final static String PACKET_ID = "packetId";
    public final static String PACKET_TYPE = "packetType";
    public final static String STORAGE_NUMBER = "storageNumber";
    public final static String INVALID_PACKET = "invalidPacket";

    @Id
    @GeneratedValue
    private Integer packetId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPacketType.class)
    @JoinColumn(name = "packetTypeId", nullable = true)
    @JsonIgnore
    private RulPacketType packetType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    @JsonIgnore
    private ArrFund fund;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    @JsonIgnore
    private String storageNumber;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonIgnore
    private State state;

    @Override
    public Integer getPacketId() {
        return packetId;
    }

    @Override
    public void setPacketId(Integer packetId) {
        this.packetId = packetId;
    }

    @Override
    public RulPacketType getPacketType() {
        return packetType;
    }

    @Override
    public void setPacketType(RulPacketType packetType) {
        this.packetType = packetType;
    }

    @Override
    public ArrFund getFund() {
        return fund;
    }

    @Override
    public void setFund(ArrFund fund) {
        this.fund = fund;
    }

    @Override
    public String getStorageNumber() {
        return storageNumber;
    }

    @Override
    public void setStorageNumber(String storageNumber) {
        this.storageNumber = storageNumber;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrPacket)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ArrPacket other = (ArrPacket) obj;

        return new EqualsBuilder().append(packetId, other.getPacketId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(packetId).toHashCode();
    }

    @Override
    public String toString() {
        return "ArrPacket pk=" + packetId;
    }
}
