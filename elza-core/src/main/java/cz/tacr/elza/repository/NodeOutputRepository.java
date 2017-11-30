package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;

/**
 * Respozitory pro vazbu výstupu na podstromy archivního popisu.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Repository
public interface NodeOutputRepository extends JpaRepository<ArrNodeOutput, Integer> {

    @Query("SELECT no FROM arr_node_output no JOIN arr_node n WHERE no.outputDefinition = ?1 and n.deleteChange is null")
    List<ArrNodeOutput> findByOutputDefinitionAndDeleteChangeIsNull(ArrOutputDefinition outputDefinition);

    @Query("SELECT no FROM arr_node_output no JOIN arr_node n WHERE no.outputDefinition = ?1 and n.createChange < ?2 and (n.deleteChange is null or n.deleteChange > ?2)")
    List<ArrNodeOutput> findByOutputDefinitionAndChange(ArrOutputDefinition outputDefinition, ArrChange lockChange);
}
