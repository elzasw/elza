package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Identifikace o přiřazených kódech původce, například IČO.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyGroupIdentifier<PU extends ParUnitdate, PG extends ParPartyGroup> extends Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getPartyGroupIdentifierId();

    void setPartyGroupIdentifierId(Integer partyGroupIdentifierId);

    PU getTo();

    void setTo(PU to);

    PU getFrom();

    void setFrom(PU from);

    PG getPartyGroup();

    void setPartyGroup(PG partyGroup);

    String getSource();

    void setSource(String source);

    String getNote();

    void setNote(String note);

    String getIdentifier();

    void setIdentifier(String identifier);

}
