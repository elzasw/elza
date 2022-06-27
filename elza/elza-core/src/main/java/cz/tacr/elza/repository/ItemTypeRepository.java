package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;


/**
 * Repository for RulItemType
 *
 */
@Repository
public interface ItemTypeRepository extends ElzaJpaRepository<RulItemType, Integer> {

    @Query("SELECT DISTINCT t.dataType FROM rul_item_type t WHERE t = ?1")
    List<RulDataType> findRulDataType(RulItemType descItemType);


    /**
     * Najde všechny typy, které mají strukturovaná data. (Jsou typu s kódem "STRUCTURED".
     *
     * @return všechny typy, které mají obal
     */
    @Query(value = "SELECT t FROM rul_item_type t join t.dataType dt "
            + "WHERE dt.code = 'STRUCTURED'")
    Set<RulItemType> findDescItemTypesForStructureds();

    /**
     * Najde všechny typy, které mají int. (Jsou typu s kódem "INT".
     *
     * @return všechny typy, které mají obal
     */
    @Query(value = "SELECT t FROM rul_item_type t join t.dataType dt "
            + "WHERE dt.code = 'INT'")
    Set<RulItemType> findDescItemTypesForIntegers();

    RulItemType findOneByCode(String code);

    List<RulItemType> findByCodeIn(Collection<String> codes);

    List<RulItemType> findByRulPackage(RulPackage rulPackage);

    @Query(value = "SELECT t FROM rul_item_type t ORDER BY t.viewOrder")
    List<RulItemType> findAllOrderByViewOrderAsc();

    List<RulItemType> findByRulPackageOrderByViewOrderAsc(RulPackage rulPackage);

    /**
     * Return item type with the highest view-order
     * @return return item with highest view_order
     */
    RulItemType findFirstByOrderByViewOrderDesc();

    void deleteByRulPackage(RulPackage rulPackage);
}
