package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;

/**
 * Respozitory pro vazbu výstupu na podstromy archivního popisu.
 *
 * @since 01.04.2016
 */
@Repository
public interface NodeOutputRepository extends JpaRepository<ArrNodeOutput, Integer> {

    String SELECT_OUTPUT_NODES = "SELECT no FROM arr_node_output no JOIN arr_level l ON l.nodeId = no.nodeId WHERE no.output = ?1";

    /**
     * Searches output nodes for output. Nodes must be valid in current fund version and also output.
     */
    @Query(SELECT_OUTPUT_NODES + " AND l.deleteChange is null AND no.deleteChange is null")
    List<ArrNodeOutput> findByOutputAndDeleteChangeIsNull(ArrOutput output);

    /**
     * Searches output nodes for output. Nodes must be valid for specified lock change in fund and also output.
     */
    @Query(SELECT_OUTPUT_NODES + " AND no.createChange < ?2 AND (l.deleteChange is null or l.deleteChange > ?2) AND (no.deleteChange is null or no.deleteChange > ?2)")
    List<ArrNodeOutput> findByOutputAndChange(ArrOutput output, ArrChange deleteChange);

    void deleteByOutputFund(ArrFund fund);
}
