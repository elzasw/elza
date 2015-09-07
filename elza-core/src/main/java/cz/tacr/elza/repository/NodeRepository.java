package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrNode;


/**
 * @author Martin Šlapa
 * @since 4. 9. 2015
 */
@Repository
public interface NodeRepository extends JpaRepository<ArrNode, Integer> {

}
