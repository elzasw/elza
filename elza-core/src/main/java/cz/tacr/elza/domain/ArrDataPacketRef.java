package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "arr_data_packet_ref")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ArrDataPacketRef extends ArrData {

    public static final String PACKET = "packet";

    @RestResource(exported = false)
	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrPacket.class)
    @JoinColumn(name = "packetId", nullable = false)
    private ArrPacket packet;

    @Column(name = "packetId", updatable = false, insertable = false)
    private Integer packetId;

	public ArrDataPacketRef() {

	}

	protected ArrDataPacketRef(ArrDataPacketRef src) {
		super(src);
		this.packet = src.packet;
		this.packetId = src.packetId;
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
        return ArrPacket.createFulltext(packet.getStorageNumber(), packet.getPacketType());
    }

	@Override
	public ArrDataPacketRef makeCopy() {
		return new ArrDataPacketRef(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataPacketRef src = (ArrDataPacketRef)srcData;
        return packetId.equals(src.packetId);
    }
}

