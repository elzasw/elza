package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repository pro {@link RulComponent}.
 *
 * @since 17.10.2017
 */
@Repository
public interface ComponentRepository extends JpaRepository<RulComponent, Integer> {

}
