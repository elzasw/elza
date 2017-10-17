package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;


/**
 * Repository for RulItemType
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @author Petr Pytelka
 * @since 20.8.2015
 */
@Repository
public interface ItemTypeRepository extends ElzaJpaRepository<RulItemType, Integer> {

    @Query("SELECT DISTINCT t.dataType FROM rul_item_type t WHERE t = ?1")
    List<RulDataType> findRulDataType(RulItemType descItemType);


    /**
     * Najde všechny typy, které mají obal. (Jsou typu s kódem "PACKET_REF".
     *
     * @return všechny typy, které mají obal
     */
    @Query(value = "SELECT t FROM rul_item_type t join t.dataType dt "
            + "WHERE dt.code = 'PACKET_REF'")
    Set<RulItemType> findDescItemTypesForPackets();

    /**
     * Najde všechny typy, které mají int. (Jsou typu s kódem "INT".
     *
     * @return všechny typy, které mají obal
     */
    @Query(value = "SELECT t FROM rul_item_type t join t.dataType dt "
            + "WHERE dt.code = 'INT'")
    Set<RulItemType> findDescItemTypesForIntegers();

    RulItemType getOneByCode(String code);


    List<RulItemType> findByRulPackage(RulPackage rulPackage);

    List<RulItemType> findByRuleSet(RulRuleSet ruleSet);

    List<RulItemType> findByRulPackageOrderByViewOrderAsc(RulPackage rulPackage);

    /**
     * Return item type with the highest view-order
     * @return return item with highest view_order
     */
    RulItemType findFirstByOrderByViewOrderDesc();


    void deleteByRulPackage(RulPackage rulPackage);


    RulItemType findOneByCode(String code);

    @Query(value = "SELECT t FROM rul_item_type t  WHERE t.code in (?1)")
    Set<RulItemType> findByCode(Set<String> descItemTypeCodes);
}
