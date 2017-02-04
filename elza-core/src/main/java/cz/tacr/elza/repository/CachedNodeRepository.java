package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrCachedNode;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 * Repository pro cachované JP.
 *
 * @author Martin Šlapa
 * @since 27.01.2017
 */
@Repository
public interface CachedNodeRepository extends ElzaJpaRepository<ArrCachedNode, Integer> {

    ArrCachedNode findOneByNodeId(Integer nodeId);

    List<ArrCachedNode> findByNodeIdIn(Collection<Integer> nodeIds);

    @Modifying
    void deleteByNodeIdIn(Collection<Integer> nodeIds);
}
