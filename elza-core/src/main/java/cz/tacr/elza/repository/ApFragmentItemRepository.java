package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApFragment;
import cz.tacr.elza.domain.ApFragmentItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.service.ByType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApFragmentItemRepository extends JpaRepository<ApFragmentItem, Integer>, ByType<ApFragment> {

    @Query("SELECT fi FROM ApFragmentItem fi LEFT JOIN FETCH fi.data d WHERE fi.deleteChange IS NULL AND fi.fragment = :fragment")
    List<ApFragmentItem> findValidItemsByFragment(@Param("fragment") ApFragment fragment);

    @Query("SELECT fi FROM ApFragmentItem fi LEFT JOIN FETCH fi.data d WHERE fi.deleteChange IS NULL AND fi.fragment = :fragment AND fi.itemType = :itemType")
    List<ApItem> findValidItemsByType(@Param("fragment") ApFragment fragment, @Param("itemType") RulItemType itemType);
}
