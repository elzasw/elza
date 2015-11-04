package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Číselník obalů.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_packet")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrPacket implements cz.tacr.elza.api.ArrPacket<ArrPacketType, ArrFindingAid> {

    @Id
    @GeneratedValue
    private Integer packetId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrPacketType.class)
    @JoinColumn(name = "packetTypeId", nullable = true)
    private ArrPacketType packetType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFindingAid.class)
    @JoinColumn(name = "findingAidId", nullable = false)
    private ArrFindingAid findingAid;

    @Column(length = 50, nullable = false)
    private String storageNumber;

    @Column(nullable = false)
    private Boolean invalidPacket;

    @Override
    public Integer getPacketId() {
        return packetId;
    }

    @Override
    public void setPacketId(Integer packetId) {
        this.packetId = packetId;
    }

    @Override
    public ArrPacketType getPacketType() {
        return packetType;
    }

    @Override
    public void setPacketType(ArrPacketType packetType) {
        this.packetType = packetType;
    }

    @Override
    public ArrFindingAid getFindingAid() {
        return findingAid;
    }

    @Override
    public void setFindingAid(ArrFindingAid findingAid) {
        this.findingAid = findingAid;
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
    public Boolean getInvalidPacket() {
        return invalidPacket;
    }

    @Override
    public void setInvalidPacket(Boolean invalidPacket) {
        this.invalidPacket = invalidPacket;
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
