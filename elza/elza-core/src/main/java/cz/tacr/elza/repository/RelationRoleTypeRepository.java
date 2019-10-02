package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParRelationRoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repozitory pro {@link ParRelationRoleType}
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 22.12.2015
 */
@Repository
public interface RelationRoleTypeRepository extends ElzaJpaRepository<ParRelationRoleType, Integer>, Packaging<ParRelationRoleType> {

    ParRelationRoleType findByCode(String roleTypeCode);

}
