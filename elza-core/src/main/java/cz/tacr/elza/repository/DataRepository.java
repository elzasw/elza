package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DataRepository extends JpaRepository<ArrData, Integer>, DataRepositoryCustom {

    @Query(value = "SELECT d FROM arr_data d join fetch d.descItem i "
            + "left join fetch i.createChange cc "
            + "left join fetch i.deleteChange dc "
            + "left join fetch i.descItemType it "
            + "left join fetch i.descItemSpec dis "
            + "WHERE i.node = ?1 and i.deleteChange is null")
    List<ArrData> findByNodeAndDeleteChangeIsNull(ArrNode node);

    @Query(value = "SELECT d FROM arr_data d join fetch d.descItem i "
            + "left join fetch i.createChange cc "
            + "left join fetch i.deleteChange dc "
            + "left join fetch i.descItemType it "
            + "left join fetch i.descItemSpec dis "
            + "WHERE i.node in (?1) and i.deleteChange is null")
    List<ArrData> findByNodesAndDeleteChangeIsNull(Collection<ArrNode> nodes);

    @Query("SELECT d FROM arr_data d join d.descItem i WHERE i.node = ?1 "
            + "and i.createChange < ?2 and (i.deleteChange is null or i.deleteChange > ?2)")
    List<ArrData> findByNodeAndChange(ArrNode node, ArrChange change);

    @Query("SELECT d FROM arr_data d join d.descItem i WHERE i.node in (?1) "
            + "and i.createChange < ?2 and (i.deleteChange is null or i.deleteChange > ?2)")
    List<ArrData> findByNodesAndChange(Collection<ArrNode> nodes, ArrChange change);


    List<ArrData> findByDescItem(ArrDescItem descItem);
}
