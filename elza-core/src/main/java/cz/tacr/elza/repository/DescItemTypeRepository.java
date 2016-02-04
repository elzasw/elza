package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPackage;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DescItemTypeRepository extends ElzaJpaRepository<RulDescItemType, Integer> {

    @Query("SELECT DISTINCT t.dataType FROM rul_desc_item_type t WHERE t = ?1")
    List<RulDataType> findRulDataType(RulDescItemType descItemType);


    /**
     * Najde všechny typy, které mají obal. (Jsou typu s kódem "PACKET_REF".
     *
     * @return všechny typy, které mají obal
     */
    @Query(value = "SELECT t FROM rul_desc_item_type t join t.dataType dt "
            + "WHERE dt.code = 'PACKET_REF'")
    Set<RulDescItemType> findDescItemTypesForPackets();

    /**
     * Najde všechny typy, které mají int. (Jsou typu s kódem "INT".
     *
     * @return všechny typy, které mají obal
     */
    @Query(value = "SELECT t FROM rul_desc_item_type t join t.dataType dt "
            + "WHERE dt.code = 'INT'")
    Set<RulDescItemType> findDescItemTypesForIntegers();

    RulDescItemType getOneByCode(String code);


    List<RulDescItemType> findByRulPackage(RulPackage rulPackage);


    void deleteByRulPackage(RulPackage rulPackage);


    RulDescItemType findOneByCode(String code);

    @Override
    default String getClassName() {
        return RulDescItemType.class.getSimpleName();
    }

    @Query(value = "SELECT t FROM rul_desc_item_type t  WHERE t.code in (?1)")
    Set<RulDescItemType> findByCode(List<String> descItemTypeCodes);
}
