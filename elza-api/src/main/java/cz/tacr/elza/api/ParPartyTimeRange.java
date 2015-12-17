package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Působnost osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyTimeRange<PP extends ParParty, PU extends ParUnitdate> extends Serializable {

    /**
     * Vlastní ID.
     * @return id
     */
    Integer getPartyTimeRangeId();

    void setPartyTimeRangeId(Integer partyTimeRangeId);

    PP getParty();

    void setParty(PP party);

    PU getFrom();

    void setFrom(PU from);

    PU getTo();

    void setTo(PU to);

}
