package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */

@Repository
public interface DaoRepository extends ElzaJpaRepository<ArrDao, Integer>, DaoRepositoryCustom {

    @Query("select count(d) from arr_dao d "
            + " join d.daoPackage dp "
            + " where dp.daoPackageId = :daoPackageId"
            + " and not exists (select dl from arr_dao_link dl where dl.dao = d and dl.deleteChange is null )")
    long countByDaoPackageIDAndNotExistsDaoLink(@Param("daoPackageId") Integer daoPackageId);

    @Query("select count(d) from arr_dao d "
            + " join d.daoPackage dp "
            + " where dp.daoPackageId = :daoPackageId")
    long countByDaoPackageID(@Param("daoPackageId") Integer daoPackageId);
}