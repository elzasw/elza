package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFsLink;
import cz.tacr.elza.domain.ArrNode;

/**
 * Repository vazeb na obsah souborového repozitáře ({@link ArrFsLink} —
 * kontejner {@link ArrDigitalRepository}, volitelný člen relativní cesta).
 */
@Repository
public interface ArrFsLinkRepository extends JpaRepository<ArrFsLink, Integer> {

    List<ArrFsLink> findByNodeIdInAndDeleteChangeIsNull(Collection<Integer> nodeIds);

    List<ArrFsLink> findByNodeAndDeleteChangeIsNull(ArrNode node);

    List<ArrFsLink> findByNodeInAndDeleteChangeIsNull(Collection<ArrNode> nodes);

    List<ArrFsLink> findByDigitalRepositoryAndPathAndDeleteChangeIsNull(ArrDigitalRepository digitalRepository,
                                                                        String path);

    List<ArrFsLink> findByDigitalRepositoryAndPathIsNullAndDeleteChangeIsNull(ArrDigitalRepository digitalRepository);

    /**
     * Returns all live filesystem links within the given repository as
     * (path, nodeId, fundId, fundName) tuples. Paths are repository-relative
     * in the canonical '/' form. Used to mark browsed items as linked and to
     * expose the linking node/fund in the browser popover. Global scope per
     * §8 item 3 of fs-repo-analysis.md — no fund predicate.
     */
    @Query("SELECT dl.path, dl.nodeId, n.fund.fundId, n.fund.name" +
            " FROM arr_fs_link dl" +
            " JOIN dl.node n" +
            " WHERE dl.digitalRepository = :digiRepo" +
            "   AND dl.deleteChange IS NULL")
    List<Object[]> findLinksByDigitalRepository(@Param("digiRepo") ArrDigitalRepository digiRepo);
}
