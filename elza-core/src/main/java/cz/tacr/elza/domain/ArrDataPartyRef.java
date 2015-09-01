package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_party_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataPartyRef extends ArrData implements cz.tacr.elza.api.ArrDataPartyRef {

    @Column(nullable = false)
    private Integer abstractPartyId;

    @Column(nullable = true)
    private Integer position;

    @Override
    public Integer getPosition() {
        return position;
    }

    @Override
    public void setPosition(Integer position) {
        this.position = position;
    }

    @Override
    public Integer getAbstractPartyId() {
        return abstractPartyId;
    }

    @Override
    public void setAbstractPartyId(Integer abstractPartyId) {
        this.abstractPartyId = abstractPartyId;
    }

    @Override
    public String getData() {
        return getPosition() + "," + getAbstractPartyId();
    }
}
