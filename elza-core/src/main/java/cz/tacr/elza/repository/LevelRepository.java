package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.FaChange;
import cz.tacr.elza.domain.FaLevel;


/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface LevelRepository extends JpaRepository<FaLevel, Integer> {

    @Query(value = "SELECT max(l.nodeId) FROM arr_fa_level l")
    Integer findMaxNodeId();

    List<FaLevel> findByParentNodeIdAndDeleteChangeIsNullOrderByPositionAsc(Integer parentNodeId);

    @Query("SELECT c FROM arr_fa_level c WHERE c.parentNodeId = ?1 "
            + "and c.createChange < ?2 and (c.deleteChange is null or c.deleteChange > ?2)"
            + " order by c.position asc")
    List<FaLevel> findByParentNodeOrderByPositionAsc(Integer parentNodeId, FaChange change);

    @Query("SELECT max(l.position) FROM arr_fa_level l WHERE l.parentNodeId = ?1 and l.deleteChange is null")
    Integer findMaxPositionUnderParent(Integer parentNodeId);

    @Query("SELECT l FROM arr_fa_level l WHERE l.parentNodeId = ?1  and l.position > ?2 and l.deleteChange is null order by l.position asc")
    List<FaLevel> findByParentNodeAndPositionGreaterThanOrderByPositionAsc(Integer getParentNodeId, Integer position);

    FaLevel findByNodeIdAndDeleteChangeIsNull(Integer levelId);

    @Query("SELECT l FROM arr_fa_level l WHERE l.nodeId = ?1 order by l.createChange.changeDate asc")
    List<FaLevel> findByNodeIdOrderByCreateChangeAsc(Integer nodeId);
}
