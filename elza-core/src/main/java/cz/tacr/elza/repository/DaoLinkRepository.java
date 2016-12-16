package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrNode;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */

@Repository
public interface DaoLinkRepository extends ElzaJpaRepository<ArrDaoLink, Integer> {

    List<ArrDaoLink> findByDaoAndNodeAndDeleteChangeIsNull(ArrDao dao, ArrNode node);

    long countByDaoAndDeleteChangeIsNull(ArrDao arrDao);
}
