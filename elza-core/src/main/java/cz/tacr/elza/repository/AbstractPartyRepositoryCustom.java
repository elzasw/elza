package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ParAbstractParty;

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
     * @return
     */
    List<ParAbstractParty> findAbstractPartyByTextAndType(String searchRecord, Integer registerTypeId, Integer firstResult, Integer maxResults);

    /**
     * 
     * @param searchRecord
     * @param registerTypeId
     * @return
     */
    long findAbstractPartyByTextAndTypeCount(String searchRecord, Integer registerTypeId);
}
