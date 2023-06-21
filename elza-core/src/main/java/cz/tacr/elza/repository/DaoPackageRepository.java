package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;

/**
 * 
 * @since 1.9.2015
 */

@Repository
public interface DaoPackageRepository extends ElzaJpaRepository<ArrDaoPackage, Integer>, DaoPackageRepositoryCustom {

    ArrDaoPackage findOneByCode(String packageIdentifier);

    @Modifying
    void deleteByFund(ArrFund fund);

    List<ArrDaoPackage> findAllByDigitalRepository(ArrDigitalRepository repository);

    List<ArrDaoPackage> findAllByDigitalRepositoryAndCodeIn(ArrDigitalRepository repository, List<String> packageIds);
}
