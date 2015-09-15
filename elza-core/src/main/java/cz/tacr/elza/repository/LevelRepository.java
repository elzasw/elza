package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Repozitory pro práci s hierarchickým stromem (level) AP.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface LevelRepository extends JpaRepository<ArrLevel, Integer> {

    List<ArrLevel> findByNodeParentAndDeleteChangeIsNullOrderByPositionAsc(ArrNode nodeParent);

    /**
     * nalezna levely podle přímého předka po zadaném číslu změny a seřadí podle pozice.
     * @param nodeParent kořen
     * @param change číslo změny
     * @return
     */
    @Query("SELECT c FROM arr_level c WHERE c.nodeParent = ?1 "
            + "and c.createChange < ?2 and (c.deleteChange is null or c.deleteChange > ?2)"
            + " order by c.position asc")
    List<ArrLevel> findByParentNodeOrderByPositionAsc(ArrNode nodeParent, ArrChange change);

    @Query("SELECT max(l.position) FROM arr_level l WHERE l.nodeParent = ?1 and l.deleteChange is null")
    Integer findMaxPositionUnderParent(ArrNode parentNode);

    @Query("SELECT l FROM arr_level l WHERE l.nodeParent = ?1  and l.position > ?2 and l.deleteChange is null order by l.position asc")
    List<ArrLevel> findByParentNodeAndPositionGreaterThanOrderByPositionAsc(ArrNode parentNode, Integer position);


    List<ArrLevel> findByNodeAndDeleteChangeIsNull(ArrNode node);

    @Query("SELECT count(l) FROM arr_level l WHERE l.node = ?1 AND l.deleteChange is null")
    Integer countByNode(ArrNode node);

    default ArrLevel findFirstByNodeAndDeleteChangeIsNull(ArrNode node) {
        List<ArrLevel> levels = findByNodeAndDeleteChangeIsNull(node);
        return levels.isEmpty() ? null : levels.iterator().next();
    }

    @Query("SELECT c FROM arr_level c WHERE c.node = ?1 "
            + "and c.createChange < ?2 and (c.deleteChange is null or c.deleteChange > ?2)"
            + " order by c.position asc")
    ArrLevel findByNodeOrderByPositionAsc(ArrNode nodeParent, ArrChange change);

    @Query("SELECT l FROM arr_level l WHERE l.node = ?1 order by l.createChange.changeDate asc")
    List<ArrLevel> findByNodeOrderByCreateChangeAsc(ArrNode node);
}
