package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaDao;

/**
 * Repository vazeb na obsah digitálního archivu ({@link ArrDaLink} —
 * kontejner {@link cz.tacr.elza.domain.DaAip}, volitelný člen {@link DaDao}).
 */
@Repository
public interface ArrDaLinkRepository extends JpaRepository<ArrDaLink, Integer> {

    List<ArrDaLink> findByNodeInAndDeleteChangeIsNull(Collection<ArrNode> nodes);

    /**
     * Finds active links that reference an AIP (or a selected part of it). The
     * AIP with its repository and the optional {@link DaDao} part are fetched
     * for export.
     */
    @Query("SELECT dl" +
            " FROM arr_da_link dl" +
            " JOIN FETCH dl.aip aip" +
            " JOIN FETCH aip.digitalRepository" +
            " LEFT JOIN FETCH dl.daDao" +
            " WHERE dl.nodeId in :nodeIds" +
            " AND dl.deleteChange is null")
    List<ArrDaLink> findAipLinksByNodeIdsAndFetchAip(@Param(value = "nodeIds") Collection<Integer> nodeIds);

    @Query("SELECT adl FROM arr_da_link adl WHERE adl.aip.aipId = :aipId AND adl.deleteChange IS NULL")
    List<ArrDaLink> findByAipIdAndDeleteChangeIsNull(@Param("aipId") Integer aipId);

    List<ArrDaLink> findByAip_AipIdAndDaDaoIsNullAndDeleteChangeIsNull(Integer aipId);

    List<ArrDaLink> findByDaDaoInAndDeleteChangeIsNull(Collection<DaDao> daDaos);

    @Query("SELECT dl" +
            " FROM arr_da_link dl" +
            " JOIN FETCH dl.aip" +
            " WHERE dl.nodeId = :nodeId" +
            " AND dl.deleteChange is null")
    List<ArrDaLink> findByNodeIdAndDeleteChangeIsNullFetchAip(@Param("nodeId") Integer nodeId);
}
