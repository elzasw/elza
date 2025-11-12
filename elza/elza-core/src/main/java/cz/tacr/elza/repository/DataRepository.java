package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.vo.DataIdTypeId;

/**
 * Repository for ArrData
 */
@Repository
public interface DataRepository extends JpaRepository<ArrData, Integer>, DataRepositoryCustom {

    @Modifying
    @Query("DELETE FROM arr_data d WHERE d.dataId IN :ids")
    void deleteByIds(@Param("ids") Collection<Integer> ids);

    @Query("SELECT d.dataId FROM arr_data d WHERE d IN (SELECT o.data FROM arr_output_item o WHERE o.output = :output)")
    Collection<Integer> findByIdsOutput(@Param("output") ArrOutput output);

    @Modifying
    @Query("DELETE FROM arr_data d WHERE d.dataId IN (SELECT i.dataId FROM arr_structured_item i WHERE i.structuredObject = :structuredObject)")
    void deleteByStructuredObject(@Param("structuredObject") ArrStructuredObject structuredObject);

    @Query("SELECT it.dataId FROM arr_item it WHERE it.itemType = ?1 and it.dataId is not null")
    List<Integer> findIdsByItemTypeFromArrItem(RulItemType dbItemType);

    @Query("SELECT it.dataId FROM ApItem it WHERE it.itemType = ?1 and it.dataId is not null")
    List<Integer> findIdsByItemTypeFromApItem(RulItemType dbItemType);

    /**
     * Change data type for give ids
     * 
     * @param ids
     *            Collection of IDS to be modified
     * @param dataTypeId
     *            New data type id
     * @return Number of modified records
     */
    @Modifying
    @Query(value = "UPDATE arr_data SET data_type_id = :dataTypeId WHERE data_id IN (:ids)", nativeQuery = true)
    int updateDataType(@Param("ids") Collection<Integer> ids, @Param("dataTypeId") Integer dataTypeId);

    /**
     * List of dataId not related to ap_item & ap_rev_item & arr_item
     * 
     * @param pageable limit on the number of results
     * @return List of DataIdTypeId
     */
    @Query("SELECT new cz.tacr.elza.repository.vo.DataIdTypeId(d.dataId, d.dataTypeId) FROM arr_data d "
    		+ "LEFT JOIN ApItem i ON i.dataId = d.dataId "
		    + "LEFT JOIN ApRevItem ri ON ri.dataId = d.dataId "
            + "LEFT JOIN arr_item ar ON ar.dataId = d.dataId "
		    + "WHERE i IS NULL AND ri IS NULL AND ar IS NULL")
    List<DataIdTypeId> findUnlinkedDataIds(Pageable pageable);
}
