package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.api.enums.InterpiClass;
import cz.tacr.elza.domain.ParInterpiMapping;

@Repository
public interface InterpiMappingRepository extends JpaRepository<ParInterpiMapping, Integer> {

    ParInterpiMapping findByInterpiClassAndInterpiRelationTypeAndInterpiRoleType(InterpiClass interpiClass,
            String interpiRelationType, String interpiRoleType);

    List<ParInterpiMapping> findByInterpiRelationType(String interpiRelationType);

    /**
     * @return mapování jejichž kombinace typu vztahu a typu role neodpovídá tabulce par_relation_type_role_type
     */
    @Query(nativeQuery = true, value = "SELECT m.interpi_mapping_id FROM par_interpi_mapping m " +
    "LEFT JOIN par_relation_type_role_type t ON m.relation_type_id = t.relation_type_id AND m.relation_role_type_id = t.role_type_id" +
            " WHERE m.relation_role_type_id IS NOT NULL AND t.relation_type_id IS NULL")
    List<Integer> findInvalidMappingIds();
}
