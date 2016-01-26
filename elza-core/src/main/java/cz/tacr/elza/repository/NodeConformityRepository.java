package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;


/**
 * Repozitář pro {@link ArrNodeConformity}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.11.2015
 */
@Repository
public interface NodeConformityRepository extends JpaRepository<ArrNodeConformity, Integer> {

    /**
     * Najde stav pro daný uzel a vybranou verzi.
     *
     * @param node    daný uzel
     * @param faVersion daná verze
     * @return stavy pro daný uzel a vybranou verzi
     */
    ArrNodeConformity findByNodeAndFaVersion(ArrNode node, ArrFindingAidVersion faVersion);


    /**
     * Najde stavy pro dané uzly a vybranou verzi.
     *
     * @param nodes   seznam uzlů
     * @param version verze stavů
     * @return stavy pro dané uzly a vybranou verzi
     */
    @Query("SELECT c FROM arr_node_conformity c WHERE c.node in (?1) "
            + "and c.faVersion = ?2")
    List<ArrNodeConformity> findByNodesAndVersion(Collection<ArrNode> nodes, ArrFindingAidVersion version);

    List<ArrNodeConformity> findByNode(ArrNode node);

    List<ArrNodeConformity> findByFaVersion(ArrFindingAidVersion deleteVersion);

    List<ArrNodeConformity> findByFaVersionAndState(ArrFindingAidVersion deleteVersion, ArrNodeConformity.State state);


    @Query("SELECT c FROM arr_node_conformity c JOIN c.node n WHERE n.nodeId in (?1) and c.faVersion = ?2")
    List<ArrNodeConformity> findByNodeIdsAndFaVersion(Collection<Integer> nodeIds, ArrFindingAidVersion version);
}
