package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDaoBatchInfo;
import org.springframework.stereotype.Repository;

/**
 * @author Martin Lebeda
 * @since 20.12.2016
 */

@Repository
public interface DaoBatchInfoRepository extends ElzaJpaRepository<ArrDaoBatchInfo, Integer> {

    ArrDaoBatchInfo findOneByCode(String identifier);

}
