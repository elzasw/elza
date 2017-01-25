package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformity.State;


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
     * @param fundVersion daná verze
     * @return stavy pro daný uzel a vybranou verzi
     */
    ArrNodeConformity findByNodeAndFundVersion(ArrNode node, ArrFundVersion fundVersion);


    /**
     * Najde stavy pro dané uzly a vybranou verzi.
     *
     * @param nodes   seznam uzlů
     * @param fundVersion verze stavů
     * @return stavy pro dané uzly a vybranou verzi
     */
    @Query("SELECT c FROM arr_node_conformity c WHERE c.node in (?1) "
            + "and c.fundVersion = ?2")
    List<ArrNodeConformity> findByNodesAndFundVersion(Collection<ArrNode> nodes, ArrFundVersion fundVersion);

    List<ArrNodeConformity> findByNode(ArrNode node);

    List<ArrNodeConformity> findByFundVersion(ArrFundVersion fundVersion);

    List<ArrNodeConformity> findByFundVersionAndState(ArrFundVersion fundVersion, ArrNodeConformity.State state);


    @Query("SELECT c FROM arr_node_conformity c JOIN c.node n WHERE n.nodeId in (?1) and c.fundVersion = ?2")
    List<ArrNodeConformity> findByNodeIdsAndFundVersion(Collection<Integer> nodeIds, ArrFundVersion fundVersion);

    @Query("SELECT COUNT(c) FROM arr_node_conformity c WHERE c.fundVersion = ?1 and c.state = ?2")
    Integer findCountByFundVersionAndState(ArrFundVersion fundVersion, State state);

    @Query("SELECT distinct c FROM arr_node_conformity c "
            + "join fetch c.node n "
            + "left join fetch c.errorConformity ec "
            + "left join fetch c.missingConformity mc "
            + "WHERE c in (?1) and c.fundVersion = ?2 and c.state = ?3 order by c.nodeConformityId asc")
    List<ArrNodeConformity> fetchErrorAndMissingConformity(List<ArrNodeConformity> nodeConformity, ArrFundVersion fundVersion, State state);

    List<ArrNodeConformity> findFirst20ByFundVersionAndStateOrderByNodeConformityIdAsc(ArrFundVersion fundVersion, State state);
}
