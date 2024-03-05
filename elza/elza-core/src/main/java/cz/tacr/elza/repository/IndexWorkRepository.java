package cz.tacr.elza.repository;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.SysIndexWork;

@Repository
@Deprecated
public interface IndexWorkRepository extends JpaRepository<SysIndexWork, Long> {

    @Query("select w from sys_index_work w where w.startTime is null")
    Page<SysIndexWork> findAllToIndex(Pageable pageable);

    @Modifying
    @Query("update sys_index_work w set w.startTime = current_timestamp where w.indexWorkId in (:ids)")
    void updateStartTime(@Param("ids") Collection<Long> workIdList);

    @Modifying
    @Query("update sys_index_work w set w.startTime = null where w.startTime is not null")
    void clearStartTime();

    @Modifying
    @Query("delete from sys_index_work w where w.indexWorkId in (:ids)")
    void delete(@Param("ids") Collection<Long> workIdList);
}
