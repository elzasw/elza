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

    @Query("SELECT c FROM arr_fa_level c WHERE c.parentNode = ?1 and c.deleteChange is null order by c.position asc")
    List<FaLevel> findByParentNodeOrderByPositionAsc(FaLevel level);

    @Query("SELECT c FROM arr_fa_level c join c.parentNode p WHERE p.faLevelId = ?1 and c.deleteChange is null order by c.position asc")
    List<FaLevel> findByParentNodeOrderByPositionAsc(Integer levelId);

    @Query("SELECT c FROM arr_fa_level c join c.parentNode p WHERE p.faLevelId = ?1 "
            + "and c.createChange < ?2 and (c.deleteChange is null or c.deleteChange > ?2)"
            + " order by c.position asc")
    List<FaLevel> findByParentNodeOrderByPositionAsc(Integer levelId, FaChange change);

    List<FaLevel> findByNodeIdAndDeleteChangeIsNullOrderByPositionAsc(Integer levelId);

    List<FaLevel> findByNodeIdOrderByPositionAsc(Integer levelId);

    @Query("SELECT c FROM arr_fa_level c WHERE c.parentNode in ?1  and c.deleteChange is null order by c.position asc")
    List<FaLevel> findByParentNodeInDeleteChangeIsNullOrderByPositionAsc(List<FaLevel> faLevels);


    FaLevel findTopByNodeIdAndDeleteChangeIsNull(Integer nodeId);

    @Query("SELECT max(l.position) FROM arr_fa_level l WHERE l.parentNode = ?1 and l.deleteChange is null")
    Integer findMaxPositionUnderParent(FaLevel parent);

    @Query("SELECT l FROM arr_fa_level l WHERE l.parentNode = ?1  and l.position > ?2 and l.deleteChange is null order by l.position asc")
    List<FaLevel> findByParentNodeAndPositionGreaterThanOrderByPositionAsc(FaLevel parentNode, Integer position);

    FaLevel findByNodeIdAndDeleteChangeIsNull(Integer levelId);
}
