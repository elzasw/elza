package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ParRelationClassType;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;


/**
 * Repozitory pro {@link ParRelationType}
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public interface RelationTypeRepository extends ElzaJpaRepository<ParRelationType, Integer>, Packaging<ParRelationType> {

    @Query("SELECT rt FROM par_relation_type rt join rt.relationClassType cls WHERE rt.code = ?1 and cls.code = ?2")
    ParRelationType findByCodeAndClassTypeCode(String relationTypeCode, String classTypeCode);

    /**
     * Najde typy vztahů podle typu role vztahu.
     *
     * @param relationRoleType typ role vztahu
     * @return seznam navázaných typů vztahu
     */
    @Query("SELECT rr.relationType FROM par_relation_type_role_type rr WHERE rr.roleType = ?1")
    List<ParRelationType> findByRelationRoleType(ParRelationRoleType relationRoleType);

    void deleteByRelationClassType(ParRelationClassType parRelationClassType);

    @Query("FROM par_relation_type rt JOIN FETCH rt.relationClassType rct")
    List<ParRelationType> findAllFetchClassType();
}
