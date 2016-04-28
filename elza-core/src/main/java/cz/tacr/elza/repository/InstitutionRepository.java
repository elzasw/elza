package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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

    ParInstitution findByCode(String institutionCode);

    ParInstitution findByParty(ParParty parParty);

}