package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrSearchWork;

@Repository
public interface SearchWorkRepository extends JpaRepository<ArrSearchWork, Integer> {

    @Query("from arr_search_work w where w.startTime is null")
    List<ArrSearchWork> findAllToIndex();

    @Modifying
    @Query("update arr_search_work w set w.startTime = current_timestamp where w.searchWorkId in (:ids)")
    void updateStartTime(@Param("ids") Collection<Integer> workIdList);

    @Modifying
    @Query("update arr_search_work w set w.startTime = null where w.startTime is not null")
    void clearStartTime();

    @Modifying
    @Query("delete from arr_search_work w where w.searchWorkId in (:ids)")
    void delete(@Param("ids") Collection<Integer> workIdList);
}
