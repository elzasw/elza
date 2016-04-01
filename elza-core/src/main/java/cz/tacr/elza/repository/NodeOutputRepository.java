package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrNodeOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Respozitory pro vazbu výstupu na podstromy archivního popisu.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Repository
public interface NodeOutputRepository extends JpaRepository<ArrNodeOutput, Integer> {

}
