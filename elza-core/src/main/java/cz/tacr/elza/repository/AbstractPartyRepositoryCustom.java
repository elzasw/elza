package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParAbstractParty;

import java.util.List;

/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface AbstractPartyRepositoryCustom {

    /**
     * 
     * @param searchRecord
     * @param registerTypeId
     * @param firstResult
     * @param maxResults
     * @param originator        původce - true, není původce - false, null - neaplikuje filtr - obě možnosti
     * @return
     */
    List<ParAbstractParty> findAbstractPartyByTextAndType(String searchRecord, Integer registerTypeId,
                                                          Integer firstResult, Integer maxResults, Boolean originator);

    /**
     * 
     * @param searchRecord
     * @param registerTypeId
     * @param originator        původce - true, není původce - false, null - neaplikuje filtr - obě možnosti
     * @return
     */
    long findAbstractPartyByTextAndTypeCount(String searchRecord, Integer registerTypeId, Boolean originator);
}
