package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;


/**
 * Repozitory pro {@link ParRelationType}
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public interface RelationTypeRepository extends JpaRepository<ParRelationType, Integer> {

    ParRelationType findByCodeAndClassType(String relationTypeCode, String classTypeCode);

    ParRelationType findByCodeAndClassTypeIsNull(String relationTypeCode);

    /**
     * Najde typy vztahů podle typu role vztahu.
     *
     * @param relationRoleType typ role vztahu
     * @return seznam navázaných typů vztahu
     */
    @Query("SELECT rr.relationType FROM par_relation_type_role_type rr WHERE rr.roleType = ?1")
    List<ParRelationType> findByRelationRoleType(ParRelationRoleType relationRoleType);
}
