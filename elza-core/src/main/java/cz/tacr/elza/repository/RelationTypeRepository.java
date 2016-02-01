package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
