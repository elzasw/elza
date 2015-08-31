package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParAbstractPartyVals
        extends Serializable {

    public Integer getRecordId();

    public void setRecordId(Integer recordId);

    public Integer getPartySubtypeId();

    public void setPartySubtypeId(Integer partySubtypeId);
}
