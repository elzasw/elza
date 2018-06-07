package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDataDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repository pro {@link ArrDataDate}
 *
 * @since 01.06.2018
 */
@Repository
public interface DataDateRepository extends JpaRepository<ArrDataDate, Integer> {

}
