package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeSpecAssign;

@Repository
public interface ItemTypeSpecAssignRepository extends JpaRepository<RulItemTypeSpecAssign, Integer> {

    @Query("SELECT COUNT(itsa) FROM RulItemTypeSpecAssign itsa WHERE itsa.itemType = :itemType")
    Long countByType(@Param("itemType") RulItemType rulItemType);

    @Query("SELECT itsa FROM RulItemTypeSpecAssign itsa JOIN FETCH itsa.itemSpec ris WHERE itsa.itemType = :itemType ORDER BY itsa.viewOrder")
    List<RulItemTypeSpecAssign> findByItemTypeSorted(@Param("itemType") RulItemType rulItemType);

    @Query("SELECT itsa FROM RulItemTypeSpecAssign itsa JOIN FETCH itsa.itemSpec ris WHERE itsa.itemType in (:itemType) ORDER BY itsa.itemType.code, itsa.viewOrder")
    List<RulItemTypeSpecAssign> findByItemTypesSorted(@Param("itemType") Collection<RulItemType> rulItemType);

    List<RulItemTypeSpecAssign> findByItemSpecIn(Collection<RulItemSpec> itemSpecs);

    List<RulItemTypeSpecAssign> findByItemTypeIn(Collection<RulItemType> itemTypes);

    void deleteByItemSpecIn(Collection<RulItemSpec> itemSpecs);

    void deleteByItemTypeIn(Collection<RulItemType> itemTypes);
}
