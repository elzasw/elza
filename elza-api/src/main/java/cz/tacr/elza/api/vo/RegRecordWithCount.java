package cz.tacr.elza.api.vo;

import cz.tacr.elza.api.RegRecord;

import java.util.List;

/**
 * Zapouzdření kolekce vracených záznamů rejstříku a jejich celkového počtu za danou vyhledávací podmínku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 * @param <RR> {@link RegRecord}
 */
public interface RegRecordWithCount<RR extends RegRecord> {

    void setRecordList(List<RR> partyList);

    List<RR> getRecordList();

    Long getCount();
}
