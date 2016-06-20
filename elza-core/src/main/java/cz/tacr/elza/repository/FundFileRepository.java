package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * ArrFile repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Repository
public interface FundFileRepository extends JpaRepository<ArrFile, Integer> {

}
