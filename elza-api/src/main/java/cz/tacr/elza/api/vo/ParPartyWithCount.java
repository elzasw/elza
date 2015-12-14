package cz.tacr.elza.api.vo;

import cz.tacr.elza.api.ParParty;

import java.util.List;

/**
 * Zapouzdření kolekce vracených osob a celkového počtu za danou vyhledávací podmínku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 * @param <PPA> {@link ParParty}
 */
public interface ParPartyWithCount<PPA extends ParParty> {

    void setRecordList(List<PPA> partyList);

    List<PPA> getRecordList();

    Long getCount();
}
