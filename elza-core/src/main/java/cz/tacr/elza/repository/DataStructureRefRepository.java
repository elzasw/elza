package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDataStructureRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * @since 16.11.2017
 */
@Repository
public interface DataStructureRefRepository extends JpaRepository<ArrDataStructureRef, Integer> {

}
