package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */

@Repository
public interface DaoFileGroupRepository extends ElzaJpaRepository<ArrDaoFileGroup, Integer> {

    List<ArrDaoFileGroup> findByDaoOrderByCodeAsc(ArrDao arrDao);

    long countByDao(ArrDao arrDao);
}
