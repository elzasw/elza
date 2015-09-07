package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrNode;


/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface LevelRepository extends JpaRepository<ArrFaLevel, Integer> {

    List<ArrFaLevel> findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(ArrNode parentNode);

    @Query("SELECT c FROM arr_fa_level c WHERE c.parentNode = ?1 "
            + "and c.createChange < ?2 and (c.deleteChange is null or c.deleteChange > ?2)"
            + " order by c.position asc")
    List<ArrFaLevel> findByParentNodeOrderByPositionAsc(ArrNode parentNode, ArrFaChange change);

    @Query("SELECT max(l.position) FROM arr_fa_level l WHERE l.parentNode = ?1 and l.deleteChange is null")
    Integer findMaxPositionUnderParent(ArrNode parentNode);

    @Query("SELECT l FROM arr_fa_level l WHERE l.parentNode = ?1  and l.position > ?2 and l.deleteChange is null order by l.position asc")
    List<ArrFaLevel> findByParentNodeAndPositionGreaterThanOrderByPositionAsc(ArrNode parentNode, Integer position);

    ArrFaLevel findByNodeAndDeleteChangeIsNull(ArrNode node);

    @Query("SELECT c FROM arr_fa_level c WHERE c.node = ?1 "
            + "and c.createChange < ?2 and (c.deleteChange is null or c.deleteChange > ?2)"
            + " order by c.position asc")
    ArrFaLevel findByNodeOrderByPositionAsc(ArrNode parentNode, ArrFaChange change);

    @Query("SELECT l FROM arr_fa_level l WHERE l.node = ?1 order by l.createChange.changeDate asc")
    List<ArrFaLevel> findByNodeOrderByCreateChangeAsc(ArrNode node);
}
