package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;

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

    @Override
    public String getFulltextValue() {
//        return (packet != null ) ? packet.getStorageNumber() : null;
        return null;
    }
}
