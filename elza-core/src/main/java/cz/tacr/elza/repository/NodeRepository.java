package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Respozitory pro nody.
 *
 * @author Martin Šlapa
 * @since 4. 9. 2015
 */
@Repository
public interface NodeRepository extends JpaRepository<ArrNode, Integer>, NodeRepositoryCustom {

    @Query("SELECT distinct n.nodeId FROM arr_node n JOIN n.policies p JOIN n.levels l WHERE l.deleteChange IS NULL AND n.fund = ?1")
    Set<Integer> findNodeIdsForFondWithPolicy(ArrFund fund);

    /**
     * Vyhledá uzly k výstupu (otevřený)
     *
     * @param output       výstup
     * @param createChange vytvoření výstupu
     * @return seznam uzlů
     */
    @Query("SELECT n FROM arr_output o JOIN o.namedOutput no JOIN no.outputNodes non JOIN non.node n WHERE o = :output AND non.createChange >= :createChange AND non.deleteChange IS NULL")
    List<ArrNode> findNodesForOutput(@Param("output") ArrOutput output,
                                     @Param("createChange") ArrChange createChange);

    /**
     * Vyhledá uzly k výstupu (uzavřený)
     *
     * @param output       výstup
     * @param createChange vytvoření výstupu
     * @param lockChange   uzavření výstupu
     * @return seznam uzlů
     */
    @Query("SELECT n FROM arr_output o JOIN o.namedOutput no JOIN no.outputNodes non JOIN non.node n WHERE o = :output AND non.createChange >= :createChange AND (non.deleteChange IS NULL OR non.deleteChange >= :lockChange)")
    List<ArrNode> findNodesForOutput(@Param("output") ArrOutput output,
                                     @Param("createChange") ArrChange createChange,
                                     @Param("lockChange") ArrChange lockChange);
}
