package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Respozitory pro vazbu výstupu na podstromy archivního popisu.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Repository
public interface NodeOutputRepository extends JpaRepository<ArrNodeOutput, Integer> {

    List<ArrNodeOutput> findByOutputDefinition(ArrOutputDefinition arrOutputDefinition);
}
