package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApFragment;
import cz.tacr.elza.domain.ApFragmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApFragmentItemRepository extends JpaRepository<ApFragmentItem, Integer> {

    @Query("SELECT fi FROM ApFragmentItem fi LEFT JOIN FETCH fi.data d WHERE fi.deleteChange IS NULL AND fi.fragment = :fragment")
    List<ApFragmentItem> findValidItemsByFragment(@Param("fragment") ApFragment fragment);

}
