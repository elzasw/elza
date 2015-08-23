package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFaChange;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DescItemRepository extends JpaRepository<ArrDescItem, Integer> {

    List<ArrDescItem> findByNodeIdAndDeleteChangeIsNull(Integer nodeId);

    @Query("SELECT i FROM arr_desc_item i WHERE i.nodeId = ?1 "
            + "and i.createChange < ?2 and (i.deleteChange is null or i.deleteChange > ?2)")
    List<ArrDescItem> findByNodeId(Integer nodeId, ArrFaChange change);
}
