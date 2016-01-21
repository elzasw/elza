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
    public String getFulltextValue() {
//        return (party != null && party.getRecord() != null) ? party.getRecord().getRecord() : null;
        return null;
    }
}
