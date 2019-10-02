package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;

/**
 * Repozitory pro práci s hierarchickým stromem (level) AP.
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface LevelRepository extends JpaRepository<ArrLevel, Integer>, LevelRepositoryCustom {

    @Query("SELECT c FROM arr_level c WHERE c.nodeParent = ?1 and c.deleteChange is null order by c.position asc")
    List<ArrLevel> findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(ArrNode nodeParent);

    /**
     * nalezna levely podle přímého předka po zadaném číslu změny a seřadí podle pozice.
     *
     * @param nodeParent kořen
     * @param change číslo změny
     * @return
     */
    @Query("SELECT c FROM arr_level c WHERE c.nodeParent = ?1 "
            + "and c.createChange < ?2 and (c.deleteChange is null or c.deleteChange > ?2) order by c.position asc")
    List<ArrLevel> findByParentNodeOrderByPositionAsc(ArrNode nodeParent, ArrChange change);

    @Query("SELECT max(l.position) FROM arr_level l WHERE l.nodeParent = ?1 and l.deleteChange is null")
    Integer findMaxPositionUnderParent(ArrNode parentNode);

    @Query("SELECT l FROM arr_level l WHERE l.nodeParent = ?1  and l.position > ?2 and l.deleteChange is null order by l.position asc")
    List<ArrLevel> findByParentNodeAndPositionGreaterThanOrderByPositionAsc(ArrNode parentNode, Integer position);

    ArrLevel findByNodeAndDeleteChangeIsNull(ArrNode node);

    /**
     * Vrací počet potomků daného uzlu.
     *
     * @param node uzel
     * @return počet potomků daného uzlu
     */
    @Query("SELECT count(l) FROM arr_level l WHERE l.nodeParent = ?1 AND l.deleteChange is null")
    Integer countChildsByParent(ArrNode node);

    /**
     * Vrací počet potomků daného uzlu.
     *
     * @param node uzel
     * @param lockChange datum uzamčení uzlu
     * @return počet potomků daného uzlu
     */
    @Query("SELECT count(l) FROM arr_level l WHERE l.nodeParent = ?1 and l.createChange < ?2 and (l.deleteChange is null or l.deleteChange > ?2)")
    Integer countChildsByParentAndChange(ArrNode node, ArrChange lockChange);

    @Query("SELECT c FROM arr_level c WHERE c.node = ?1 and c.createChange < ?2 and (c.deleteChange is null or c.deleteChange > ?2)")
    ArrLevel findByNodeAndNotNullLockChange(ArrNode node, ArrChange lockChange);

    @Query("SELECT l FROM arr_level l WHERE l.node = ?1 order by l.createChange.changeDate asc")
    List<ArrLevel> findByNodeOrderByCreateChangeAsc(ArrNode node);

    @Query("SELECT l FROM arr_level l WHERE l.nodeParent = ?1")
    List<ArrLevel> findByParentNode(ArrNode node);

    @Modifying
    @Query("DELETE FROM arr_level l WHERE l.node IN (SELECT n FROM arr_node n WHERE n.fund = ?1)")
    void deleteByNodeFund(ArrFund fund);
}
