package cz.tacr.elza.interpi.service.vo;

import cz.tacr.elza.controller.vo.RegScopeVO;

/**
 * Informace o existujícím rejstříku.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public class PairedRecordVO {

    /** Scope ve kterém se rejstřík nachází. */
    private RegScopeVO scope;

    /** Id rejstříku. */
    private Integer recordId;

    public PairedRecordVO(final RegScopeVO scope, final Integer recordId) {
        this.scope = scope;
        this.recordId = recordId;
    }

    public RegScopeVO getScope() {
        return scope;
    }

    public Integer getRecordId() {
        return recordId;
    }
}
