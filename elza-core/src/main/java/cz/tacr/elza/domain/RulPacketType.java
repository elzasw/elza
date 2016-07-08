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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Číselník typů obalů.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "rul_packet_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class RulPacketType implements cz.tacr.elza.api.RulPacketType<RulPackage> {

    public final static String PACKET_TYPE_ID = "packetTypeId";
    public static final String NAME = "name";

    @Id
    @GeneratedValue
    private Integer packetTypeId;

    @Column(length = 50, nullable = false)
    private String code;
    
    @Column(length = 250, nullable = false)
    private String name;
    
    @Column(length = 50, nullable = false)
    private String shortcut;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Override
    public Integer getPacketTypeId() {
        return packetTypeId;
    }

    @Override
    public void setPacketTypeId(Integer packetTypeId) {
        this.packetTypeId = packetTypeId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getShortcut() {
        return shortcut;
    }

    @Override
    public void setShortcut(String shortcut) {
        this.shortcut = shortcut;
    }

    @Override
    public RulPackage getPackage() {
        return rulPackage;
    }

    @Override
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulPacketType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RulPacketType other = (RulPacketType) obj;

        return new EqualsBuilder().append(packetTypeId, other.getPacketTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(packetTypeId).append(name).append(code).append(shortcut).toHashCode();
    }

    @Override
    public String toString() {
        return "ArrPacketType pk=" + packetTypeId;
    }
}
