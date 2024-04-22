package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;

/**
 * Respozitory pro archivní soubory
 * 
 */
@Repository
public interface FundRepository extends ElzaJpaRepository<ArrFund, Integer> , FundRepositoryCustom {

    @Query("SELECT fa FROM arr_fund fa JOIN fa.outputs o WHERE o.outputId = ?1")
    ArrFund findByOutputId(Integer outputId);

    @Query("SELECT fa FROM arr_fund fa JOIN fa.institution inst WHERE inst.institutionId = ?1")
    List<ArrFund> findByInstitutionId(Integer institutionId);

    @Query("SELECT up FROM usr_permission_view up WHERE up.userId = ?1")
    List<ArrFund> findFromUsrPermissionByUserId(Integer userId);
}
