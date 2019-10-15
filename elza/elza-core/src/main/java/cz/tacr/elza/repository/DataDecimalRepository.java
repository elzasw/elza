package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataDecimal;


/**
 * @author Martin Šlapa
 * @since 12.10.2015
 */
@Repository
public interface DataDecimalRepository extends JpaRepository<ArrDataDecimal, Integer> {

}
