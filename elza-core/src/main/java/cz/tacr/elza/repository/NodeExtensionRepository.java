package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrNodeExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repository pro {@link ArrNodeExtension}.
 *
 * @since 23.10.2017
 */
@Repository
public interface NodeExtensionRepository extends JpaRepository<ArrNodeExtension, Integer>, NodeExtensionRepositoryCustom {

}
