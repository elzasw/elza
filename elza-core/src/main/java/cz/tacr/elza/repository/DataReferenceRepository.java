package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataReference;


/**
 * @author Martin Šlapa
 * @since 27.8.2015
 */
@Repository
public interface DataReferenceRepository extends JpaRepository<ArrDataReference, Integer> {

}
