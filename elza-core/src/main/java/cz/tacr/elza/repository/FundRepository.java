package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Respozitory pro Archivní pomůcku.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface FundRepository extends ElzaJpaRepository<ArrFund, Integer> , FundRepositoryCustom {

    @Query(value = "SELECT fa FROM arr_fund_version v JOIN v.fund fa JOIN v.rootNode n where n.uuid = ?1 and v.lockChange is null")
    ArrFund findByRootNodeUuid(String uuid);

    @Query("SELECT fa FROM arr_fund fa JOIN fa.outputDefinitions o WHERE o.outputDefinitionId=?1")
    ArrFund findByOutputDefinitionId(Integer outputDefinitionId);
}
