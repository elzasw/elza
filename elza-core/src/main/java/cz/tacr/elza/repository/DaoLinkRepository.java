package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */

@Repository
public interface DaoLinkRepository extends ElzaJpaRepository<ArrDaoLink, Integer> {

    List<ArrDaoLink> findByDaoAndNodeAndDeleteChangeIsNull(ArrDao dao, ArrNode node);

    List<ArrDaoLink> findByDaoAndDeleteChangeIsNull(ArrDao dao);

    List<ArrDaoLink> findByDao(ArrDao arrDao);

    List<ArrDaoLink> findByNodeIdInAndDeleteChangeIsNull(Collection<Integer> nodeIds);

    @Modifying
    void deleteByNode(ArrNode node);

    void deleteByNodeFund(ArrFund fund);
}
