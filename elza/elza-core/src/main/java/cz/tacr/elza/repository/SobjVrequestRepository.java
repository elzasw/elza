package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrSobjVrequest;

/**
 * Repository for ArrSobjVrequest
 * 
 *
 */
@Repository
public interface SobjVrequestRepository extends JpaRepository<ArrSobjVrequest, Integer> {

}
