package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ArrDataUriRef;

import cz.tacr.elza.domain.RulPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

import static cz.tacr.elza.domain.factory.DescItemFactory.ELZA_NODE;

/**
 * @author Tomáš Gotzy
 * @since 28.02.2002 *
 */
public interface DataUriRefRepository extends JpaRepository<ArrDataUriRef, Integer> {

    @Modifying
    @Query("UPDATE ArrDataUriRef ur SET ur.nodeId=null WHERE ur.nodeId in ?1")
    void updateByNodesIdIn(Collection<Integer> nodeIds);

    @Query("SELECT ur FROM ArrDataUriRef ur WHERE ur.value = ?1")
    List<ArrDataUriRef> findAllByNodeUUID(String value);

    @Query("SELECT ur FROM ArrDataUriRef ur WHERE ur.schema = '" + ELZA_NODE + "' AND ur.arrNode IS NULL")
    Page<ArrDataUriRef> findByUnresolvedNodeRefs(Pageable pageable);

}
