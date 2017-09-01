package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.ParPartyNameFormType;

/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */

public interface PartyNameFormTypeRepository extends JpaRepository<ParPartyNameFormType, Integer>, Packaging<ParPartyNameFormType> {

    ParPartyNameFormType findByCode(String partyNameFormTypeCode);

    ParPartyNameFormType findByName(String partyNameFormTypeName);
}
