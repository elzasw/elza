package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityInfo;


/**
 * Repozitář pro {@link ArrNodeConformityInfo}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.11.2015
 */
@Repository
public interface NodeConformityInfoRepository extends JpaRepository<ArrNodeConformityInfo, Integer> {

    /**
     * Najde stav pro daný uzel a vybranou verzi.
     *
     * @param node    daný uzel
     * @param faVersion daná verze
     * @return stavy pro daný uzel a vybranou verzi
     */
    ArrNodeConformityInfo findByNodeAndFaVersion(ArrNode node, ArrFindingAidVersion faVersion);


    /**
     * Najde stavy pro dané uzly a vybranou verzi.
     *
     * @param nodes   seznam uzlů
     * @param version verze stavů
     * @return stavy pro dané uzly a vybranou verzi
     */
    @Query("SELECT c FROM arr_node_conformity_info c WHERE c.node in (?1) "
            + "and c.faVersion = ?2")
    List<ArrNodeConformityInfo> findByNodesAndVersion(Collection<ArrNode> nodes, ArrFindingAidVersion version);
}
