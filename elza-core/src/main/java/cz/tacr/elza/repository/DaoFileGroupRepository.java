package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */

@Repository
public interface DaoFileGroupRepository extends ElzaJpaRepository<ArrDaoFileGroup, Integer> {

    List<ArrDaoFileGroup> findByDaoOrderByCodeAsc(ArrDao arrDao);

    long countByDao(ArrDao arrDao);

    @Modifying
    @Query("DELETE FROM arr_dao_file_group fg WHERE fg.daoId IN (SELECT d.daoId FROM arr_dao d WHERE d.daoPackageId IN (SELECT p.daoPackageId FROM arr_dao_package p WHERE p.fund = ?1))")
    void deleteByFund(ArrFund fund);

    @Query("SELECT g FROM arr_dao_file_group g WHERE g.code in :codes")
    List<ArrDaoFileGroup> findByCodes(@Param(value = "codes") Collection<String> codes);
}
