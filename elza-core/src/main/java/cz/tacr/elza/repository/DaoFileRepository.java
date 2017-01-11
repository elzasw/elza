package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */

@Repository
public interface DaoFileRepository extends ElzaJpaRepository<ArrDaoFile, Integer> {

    List<ArrDaoFile> findByDaoAndDaoFileGroupIsNull(ArrDao dao);

    long countByDaoAndDaoFileGroupIsNull(ArrDao arrDao);

    List<ArrDaoFile> findByDaoAndDaoFileGroup(ArrDao arrDao, ArrDaoFileGroup daoFileGroup);

    List<ArrDaoFile> findByDao(ArrDao arrDao);
}
