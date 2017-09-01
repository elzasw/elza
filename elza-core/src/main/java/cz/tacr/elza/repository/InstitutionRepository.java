package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;


/**
 * Repository pro {@link ParInstitution}.
 *
 * @author Martin Šlapa
 * @since 18.3.2016
 */
@Repository
public interface InstitutionRepository extends JpaRepository<ParInstitution, Integer> {

    ParInstitution findByInternalCode(String institutionInternalCode);

    ParInstitution findByParty(ParParty parParty);

    List<ParInstitution> findByPartyIdIn(Collection<Integer> partyIds);

    @Modifying
    int deleteByInstitutionIdIn(Collection<Integer> partyIds);
}