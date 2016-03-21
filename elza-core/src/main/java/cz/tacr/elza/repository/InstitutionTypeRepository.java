package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParInstitutionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repository pro {@link ParInstitutionType}.
 *
 * @author Martin Šlapa
 * @since 18.3.2016
 */
@Repository
public interface InstitutionTypeRepository extends JpaRepository<ParInstitutionType, Integer> {

}