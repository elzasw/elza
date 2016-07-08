package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParRelationRoleType;


/**
 * Repozitory pro {@link ParRelationRoleType}
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 22.12.2015
 */
@Repository
public interface RelationRoleTypeRepository extends JpaRepository<ParRelationRoleType, Integer> {

    ParRelationRoleType findByCode(String roleTypeCode);

}
