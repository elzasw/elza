package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParUnitdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repozitory pro {@link ParUnitdate}
 */
@Repository
public interface PartyUnitdateRepository extends JpaRepository<ParUnitdate, Integer> {

}
