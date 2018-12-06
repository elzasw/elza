package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.RulItemType;

@Repository
public interface ApItemRepository extends JpaRepository<ApItem, Integer> {

    @Query("SELECT COUNT(i) FROM ApItem i WHERE i.itemType = ?1")
    long getCountByType(RulItemType dbItemType);

}
