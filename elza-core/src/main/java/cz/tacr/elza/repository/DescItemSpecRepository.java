package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulDescItemSpec;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DescItemSpecRepository extends JpaRepository<RulDescItemSpec, Integer> {

}
