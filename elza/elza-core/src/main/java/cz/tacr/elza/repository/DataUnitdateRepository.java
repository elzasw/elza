package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataUnitdate;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Repository
public interface DataUnitdateRepository extends JpaRepository<ArrDataUnitdate, Integer> {

}
