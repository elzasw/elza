package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParInstitutionType;


/**
 * Repository pro {@link ParInstitutionType}.
 *
 * @author Martin Šlapa
 * @since 18.3.2016
 */
@Repository
public interface InstitutionTypeRepository extends JpaRepository<ParInstitutionType, Integer>, Packaging<ParInstitutionType> {

    ParInstitutionType findByCode(String typeCode);

}