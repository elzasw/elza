package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataNull;


/**
 * @author Martin Šlapa
 * @since 18.11.15
 */
@Repository
public interface DataNullRepository extends JpaRepository<ArrDataNull, Integer> {

}
