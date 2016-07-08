package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataCoordinates;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Repository
public interface DataCoordinatesRepository extends JpaRepository<ArrDataCoordinates, Integer> {

}
