package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.ParRelationTypeRoleType;

/**
 * Repozitory pro {@link ParRelationTypeRoleType}
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 22.12.2015
 */
@Repository
public interface RelationTypeRoleTypeRepository extends JpaRepository<ParRelationTypeRoleType, Integer>, Packaging<ParRelationTypeRoleType> {

    @Query("FROM par_relation_type_role_type rrt JOIN FETCH rrt.roleType rt")
    List<ParRelationTypeRoleType> findAllFetchRoleType();

    void deleteByRoleType(ParRelationRoleType parRelationRoleTypesDelete);

    void deleteByRelationType(ParRelationType parRelationType);
}
