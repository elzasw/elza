package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_party_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataPartyRef extends ArrData implements cz.tacr.elza.api.ArrDataPartyRef {

    @Column(nullable = false)
    private Integer partyId;

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
    public Integer getPartyId() {
        return partyId;
    }

    @Override
    public void setPartyId(Integer partyId) {
        this.partyId = partyId;
    }

    @Override
    public String getData() {
        return getPartyId()+"";
//        return getPosition() + "," + getPartyId();
    }
}
