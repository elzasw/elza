package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Respozitory pro Archivní pomůcku.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface FundRepository extends ElzaJpaRepository<ArrFund, Integer> , FundRepositoryCustom {

    @Query(value = "select fa from arr_fund_version v join v.fund fa join v.rootNode n where n.uuid = :uuid and v.lockChange is null")
    ArrFund findFundByRootNodeUUID(@Param(value = "uuid") String uuid);

    @Query("SELECT fa FROM arr_fund fa JOIN fa.outputDefinitions o WHERE o.outputDefinitionId=?1")
    ArrFund findByOutputDefinitionId(Integer outputDefinitionId);

    ArrFund findOneByUuid(String uuid);

}
