package cz.tacr.elza.controller.vo.nodes.descitems;


import java.util.Objects;

import javax.persistence.EntityManager;

import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * VO hodnoty atributu - party.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemPartyRefVO extends ArrItemVO {

    /**
     * osoba
     */
    private ParPartyVO party;

    private Integer value;

    public ParPartyVO getParty() {
        return party;
    }

    public void setParty(final ParPartyVO party) {
        this.party = party;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    // Entity can be created only from ID and not from embedded object
    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataPartyRef data = new ArrDataPartyRef();

        if (party != null) {
            if (!Objects.equals(party.getId(), value)) {
                throw new BusinessException("Inconsistent data, party is not null", BaseCode.PROPERTY_IS_INVALID)
                        .set("value", value).set("party.id", party.getId());
            }
        }

        // try to map party
        ParParty party = null;
        if (this.value != null) {
            party = em.getReference(ParParty.class, value);
        }
        data.setParty(party);

        data.setDataType(DataType.PARTY_REF.getEntity());
        return data;
    }
}