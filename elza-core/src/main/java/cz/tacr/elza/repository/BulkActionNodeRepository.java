package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrBulkActionNode;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository k {@link ArrBulkActionNode}
 *
 * @author Martin Šlapa
 * @since 04.04.2016
 */
public interface BulkActionNodeRepository extends JpaRepository<ArrBulkActionNode, Integer> {

}
