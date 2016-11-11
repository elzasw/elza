package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.ParPartyType;

/**
 * Repository pro typy osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PartyTypeRepository extends ElzaJpaRepository<ParPartyType, Integer> {

    ParPartyType findPartyTypeByCode(String partyTypeCode);

}
