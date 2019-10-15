package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParPartyType;

/**
 * Repository pro typy osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PartyTypeRepository extends ElzaJpaRepository<ParPartyType, Integer> {

    ParPartyType findPartyTypeByCode(String partyTypeCode);

}
