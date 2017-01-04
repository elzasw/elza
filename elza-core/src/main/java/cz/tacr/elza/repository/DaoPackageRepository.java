package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDaoPackage;
import org.springframework.stereotype.Repository;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */

@Repository
public interface DaoPackageRepository extends ElzaJpaRepository<ArrDaoPackage, Integer>, DaoPackageRepositoryCustom {

    ArrDaoPackage findOneByCode(String packageIdentifier);
}
