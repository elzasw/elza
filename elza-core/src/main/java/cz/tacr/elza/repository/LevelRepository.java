package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.FaLevel;


/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface LevelRepository extends JpaRepository<FaLevel, Integer> {

    @Query(value = "SELECT max(l.nodeId) FROM arr_fa_level l")
    Integer findMaxNodeId();


    @Query("SELECT c FROM arr_fa_level c WHERE c.parentNode = ?1 order by c.position asc")
    List<FaLevel> findByParentNodeOrderByPositionAsc(FaLevel level);


    List<FaLevel> findByNodeIdOrderByPositionAsc(Integer levelId);


    List<FaLevel> findByParentNodeIn(List<FaLevel> faLevels);


    List<FaLevel> findByParentNodeInOrderByPositionAsc(List<FaLevel> faLevels);
}
