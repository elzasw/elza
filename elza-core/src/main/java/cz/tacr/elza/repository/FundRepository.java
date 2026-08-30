package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ParInstitution;

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

    @Query("SELECT up.fund FROM usr_permission_view up WHERE up.userId = ?1")
    List<ArrFund> findFromUsrPermissionByUserId(Integer userId);

    @Query("SELECT af FROM arr_fund af WHERE af.internalCode = ?1")
    ArrFund findByInternalCode(String code);

    /**
     * Funds of the institution carrying the given fund number. The pair is not unique in the
     * database, so the caller decides what an ambiguous result means.
     */
    @Query("SELECT af FROM arr_fund af WHERE af.institution = ?1 AND af.fundNumber = ?2")
    List<ArrFund> findByInstitutionAndFundNumber(ParInstitution institution, Integer fundNumber);

    @Query("SELECT f FROM arr_fund_version fv JOIN fv.fund f WHERE fv.lockChangeId IS NULL AND fv.fundVersionId = ?1")
    ArrFund findByFundVersionId(Integer fundVersionId);
}
