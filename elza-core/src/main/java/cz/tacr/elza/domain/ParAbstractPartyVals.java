package cz.tacr.elza.domain;

/**
 * Abstraktní osoby.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */

public class ParAbstractPartyVals implements cz.tacr.elza.api.ParAbstractPartyVals {

    private Integer recordId;
    private Integer partySubtypeId;

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    public Integer getPartySubtypeId() {
        return partySubtypeId;
    }

    public void setPartySubtypeId(Integer partySubtypeId) {
        this.partySubtypeId = partySubtypeId;
    }

}
