package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFaChange;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DataRepository extends JpaRepository<ArrData, Integer> {

    @Query(value = "SELECT d FROM arr_data d join fetch d.descItem i "
            + "left join fetch i.createChange cc "
            + "left join fetch i.deleteChange dc "
            + "left join fetch i.descItemType it "
            + "left join fetch i.descItemSpec dis "
            + "WHERE i.nodeId = ?1 and i.deleteChange is null")
    List<ArrData> findByNodeIdAndDeleteChangeIsNull(Integer nodeId);

    @Query(value = "SELECT d FROM arr_data d join fetch d.descItem i "
            + "left join fetch i.createChange cc "
            + "left join fetch i.deleteChange dc "
            + "left join fetch i.descItemType it "
            + "left join fetch i.descItemSpec dis "
            + "WHERE i.nodeId in (?1) and i.deleteChange is null")
    List<ArrData> findByNodeIdsAndDeleteChangeIsNull(Collection<Integer> nodeId);

    @Query("SELECT d FROM arr_data d join d.descItem i WHERE i.nodeId = ?1 "
            + "and i.createChange < ?2 and (i.deleteChange is null or i.deleteChange > ?2)")
    List<ArrData> findByNodeIdAndChange(Integer nodeId, ArrFaChange change);

    @Query("SELECT d FROM arr_data d join d.descItem i WHERE i.nodeId in (?1) "
            + "and i.createChange < ?2 and (i.deleteChange is null or i.deleteChange > ?2)")
    List<ArrData> findByNodeIdsAndChange(Collection<Integer> nodeId, ArrFaChange change);


    List<ArrData> findByDescItem(ArrDescItem descItem);
}
