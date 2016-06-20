package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrOutputResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * OutputResult repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Repository
public interface OutputResultRepository extends JpaRepository<ArrOutputResult, Integer> {

}
