package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * ArrFile repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Repository
public interface FundFileRepository extends ElzaJpaRepository<ArrFile,Integer>, FundFileRepositoryCustom {

    List<ArrFile> findByFund(ArrFund fund);
}
