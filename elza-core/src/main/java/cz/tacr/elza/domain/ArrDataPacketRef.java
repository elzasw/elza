package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;



/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_packet_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataPacketRef extends ArrData implements cz.tacr.elza.api.ArrDataPacketRef {

    @Column(nullable = false)
    private Integer packetId;

    @Override
    public Integer getPacketId() {
        return packetId;
    }

    @Override
    public void setPacketId(Integer packetId) {
        this.packetId = packetId;
    }
}
