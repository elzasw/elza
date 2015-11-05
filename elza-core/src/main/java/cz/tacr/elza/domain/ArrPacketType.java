package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Číselník typů obalů.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_packet_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrPacketType implements cz.tacr.elza.api.ArrPacketType {

    public final static String PACKET_TYPE_ID = "packetTypeId";

    @Id
    @GeneratedValue
    private Integer packetTypeId;

    @Column(length = 50, nullable = false)
    private String code;
    
    @Column(length = 250, nullable = false)
    private String name;
    
    @Column(length = 50, nullable = false)
    private String shortcut;

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
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrPacketType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ArrPacketType other = (ArrPacketType) obj;

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
