package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.api.InterpiClass;
import cz.tacr.elza.domain.ParInterpiMapping;

@Repository
public interface InterpiMappingRepository extends JpaRepository<ParInterpiMapping, Integer> {

    ParInterpiMapping findByInterpiClassAndInterpiRelationTypeAndInterpiRoleType(InterpiClass interpiClass,
            String interpiRelationType, String interpiRoleType);

}
