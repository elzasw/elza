package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputResult;
import org.springframework.stereotype.Repository;


/**
 * OutputResult repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Repository
public interface OutputResultRepository extends ElzaJpaRepository<ArrOutputResult, Integer> {

    ArrOutputResult findByOutputDefinition(final ArrOutputDefinition outputDefinition);
}
