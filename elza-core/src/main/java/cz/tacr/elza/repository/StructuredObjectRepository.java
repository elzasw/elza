package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;

/**
 * Repozitory pro {@link ArrStructuredObject}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructuredObjectRepository extends JpaRepository<ArrStructuredObject, Integer>, StructuredObjectRepositoryCustom {

    List<ArrStructuredObject> findByStructuredTypeAndFundAndDeleteChangeIsNull(RulStructuredType structuredType, ArrFund fund);

    @Query("SELECT sd FROM arr_structured_object sd WHERE sd.structuredType = :structuredType "
            + "AND sd.fund = :fund AND sd.deleteChange IS NULL "
            + "AND sd.sortValue = :sortValue AND sd.state in ('OK','ERROR') "
            + "AND sd <> :ignoreSobj")
    List<ArrStructuredObject> findValidByStructureTypeAndFund(@Param("structuredType") RulStructuredType structuredType,
                                                              @Param("fund") ArrFund fund,
                                                              @Param("sortValue") String sortValue,
                                                              @Param("ignoreSobj") ArrStructuredObject ignoreSobj);

    @Query("SELECT sd.structuredObjectId FROM arr_structured_object sd WHERE sd.structuredType = :structuredType "
            + "AND sd.fund = :fund AND sd.deleteChange IS NULL AND sd.state in ('OK','ERROR')")
    List<Integer> findStructuredObjectIdByStructureTypeFund(@Param("structuredType") RulStructuredType structuredType,
                                                            @Param("fund") ArrFund fund);

    @Query("SELECT sd FROM arr_structured_object sd JOIN FETCH sd.structuredType WHERE sd.structuredObjectId = :structuredObjectId")
    ArrStructuredObject findOneFetch(@Param("structuredObjectId") Integer structuredObjectId);

    @Query("SELECT sd FROM arr_structured_object sd JOIN FETCH sd.structuredType WHERE sd.structuredObjectId IN :structuredObjectId")
    List<ArrStructuredObject> findByIdsFetch(@Param("structuredObjectId") List<Integer> structuredObjectId);

    @Query("SELECT sd.structuredObjectId FROM arr_structured_object sd WHERE sd.structuredType IN :structuredTypes AND sd.deleteChange IS NULL")
    List<Integer> findStructuredObjectIdByStructureTypes(@Param("structuredTypes") Collection<RulStructuredType> structuredTypes);

    @Query("SELECT sd.structuredObjectId FROM arr_structured_object sd JOIN sd.structuredType st " +
            "JOIN rul_structured_type_extension se ON se.structuredType = st JOIN arr_fund_structure_extension fse ON fse.structuredTypeExtension = se " +
            "WHERE se IN :structuredTypeExtensions AND fse.fund = sd.fund AND fse.deleteChange IS NULL AND sd.deleteChange IS NULL")
    List<Integer> findStructuredObjectIdByActiveStructureExtensions(@Param("structuredTypeExtensions") Collection<RulStructuredTypeExtension> structuredTypeExtensions);

    @Modifying
    @Query("DELETE FROM arr_structured_object WHERE state = 'TEMP'")
    void deleteByStateTemp();

    @Query("SELECT sd.createChange FROM arr_structured_object sd WHERE sd.state = 'TEMP'")
    List<ArrChange> findTempChange();

    @Query("SELECT sd.createChange FROM arr_structured_object sd WHERE sd.state = 'TEMP' AND sd = :structuredObject")
    ArrChange findTempChangeByStructuredObject(@Param("structuredObject") ArrStructuredObject structuredObject);

    List<ArrStructuredObject> findByFundAndDeleteChangeIsNull(ArrFund fund);
    
    List<ArrStructuredObject> findByFund(ArrFund fund);

    @Modifying
    @Query("DELETE FROM arr_structured_object so WHERE so.fund = ?1")
    void deleteByFund(ArrFund fund);

    @Query("SELECT DISTINCT so FROM arr_structured_object so " +
            "JOIN arr_data_structure_ref ds ON ds.structuredObject = so " +
            "WHERE so.state = 'TEMP'")
    List<ArrStructuredObject> findConnectedTempObjs();

    /**
     * Count number of structured objects within two changeIds
     * 
     * @param fundId
     * @param fromChange
     *            Higher change id
     * @param toChange
     *            Lower change id
     * @return
     */
    @Query("SELECT COUNT(so) FROM arr_structured_object so WHERE so.fundId = :fundId and " +
            "( " +
            " (so.createChangeId>=:toChangeId AND so.createChangeId<=:fromChangeId) OR " +
            " (so.deleteChangeId is not null AND so.deleteChangeId>=:toChangeId AND so.deleteChangeId<=:fromChangeId) "
            +
            ")")
    int countItemsWithinChangeRange(@Param("fundId") Integer fundId,
                                    @Param("fromChangeId") Integer fromChangeId,
                                    @Param("toChangeId") Integer toChangeId);

}
