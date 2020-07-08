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

    @Query("SELECT sd FROM arr_structured_object sd JOIN FETCH sd.structuredType WHERE sd.uuid = :uuid AND sd.deleteChange IS NULL")
    ArrStructuredObject findActiveByUuidOneFetch(@Param("uuid") String uuid);

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

    @Query("SELECT so" +
            " FROM arr_structured_object so" +
            " WHERE so.fund.id = :fundId" +
            " AND so.deleteChange.id in :deleteChangeIds" +
            " AND so.state <> 'TEMP'" +
            " ORDER BY so.sortValue")
    List<ArrStructuredObject> findByFundAndDeleteChange(@Param("fundId") Integer fundId, @Param("deleteChangeIds") List<Integer> deleteChangeIds);

    @Query("SELECT so" +
            " FROM arr_structured_object so" +
            " WHERE so.fund.id = :fundId" +
            " AND so.createChange.id in :createChangeIds" +
            " AND so.state <> 'TEMP'" +
            " ORDER BY so.sortValue")
    List<ArrStructuredObject> findByFundAndCreateChange(@Param("fundId") Integer fundId, @Param("createChangeIds") List<Integer> createChangeIds);

    @Query("SELECT so" +
            " FROM arr_structured_object so" +
            " WHERE so.fund = :fund" +
            " AND so.deleteChange >= :change")
    List<ArrStructuredObject> findNotBeforeDeleteChange(@Param("fund") ArrFund fund, @Param("change") ArrChange change);

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

    public interface MaximalItemValues {
        String getPrefix();

        Integer getValue();
    }

    @Query(nativeQuery = true, value = "select pref_ds.value AS prefix, max(di.value) as value from arr_structured_object so "
            +
            "join arr_structured_item si on si.structured_object_id = so.structured_object_id " +
            "join arr_item i on i.item_id = si.item_id and i.delete_change_id is null and i.item_type_id = :numberItemTypeId "
            +
            "join arr_data_integer di on di.data_id = i.data_id " +
            "left join arr_structured_item pref_si on pref_si.structured_object_id = so.structured_object_id " +
            "left join arr_item pref_i on pref_i.item_id = pref_si.item_id and pref_i.delete_change_id is null and pref_i.item_type_id = :prefixItemTypeId "
            +
            "left join arr_data_string pref_ds on pref_ds.data_id  = pref_i.data_id " +
            "where so.fund_id = :fundId and so.delete_change_id is null "
            +
            "group by pref_ds.value")
    List<MaximalItemValues> countMaximalItemValues(@Param("fundId") Integer fundId,
                                               @Param("prefixItemTypeId") Integer prefixItemTypeId,
                                               @Param("numberItemTypeId") Integer numberItemTypeId);
}
