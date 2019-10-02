package cz.tacr.elza.interpi.service.vo;

import cz.tacr.elza.controller.vo.ApScopeVO;

/**
 * Informace o existujícím rejstříku.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public class PairedRecordVO {

    /** Scope ve kterém se rejstřík nachází. */
    private ApScopeVO scope;

    /** Id rejstříku. */
    private Integer recordId;

    /** Id osoby. */
    private Integer partyId;

    public PairedRecordVO(final ApScopeVO scope, final Integer recordId, final Integer partyId) {
        this.scope = scope;
        this.recordId = recordId;
        this.partyId = partyId;
    }

    public ApScopeVO getScope() {
        return scope;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public Integer getPartyId() {
        return partyId;
    }
}
