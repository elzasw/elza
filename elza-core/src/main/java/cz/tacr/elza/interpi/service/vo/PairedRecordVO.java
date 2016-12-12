package cz.tacr.elza.interpi.service.vo;

import cz.tacr.elza.domain.RegScope;

/**
 * Informace o existujícím rejstříku.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public class PairedRecordVO {

    /** Scope ve kterém se rejstřík nachází. */
    private RegScope scope;

    /** Id rejstříku. */
    private Integer recordId;

    public PairedRecordVO(final RegScope scope, final Integer recordId) {
        this.scope = scope;
        this.recordId = recordId;
    }

    public RegScope getScope() {
        return scope;
    }

    public Integer getRecordId() {
        return recordId;
    }
}
