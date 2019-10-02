package cz.tacr.elza.controller.vo.ap.item;


import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

import javax.persistence.EntityManager;
import java.util.Objects;


/**
 * @since 18.07.2018
 */
public class ApItemPartyRefVO extends ApItemVO {

    /**
     * osoba
     */
    private ParPartyVO party;

    private Integer value;

    public ApItemPartyRefVO() {
    }

    public ApItemPartyRefVO(final ApItem item) {
        super(item);
        ArrDataPartyRef data = (ArrDataPartyRef) item.getData();
        value = data == null ? null : data.getPartyId();
    }

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
