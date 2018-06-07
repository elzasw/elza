package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrFund;


/**
 * @since 16.11.2017
 */
@Repository
public interface DataStructureRefRepository extends JpaRepository<ArrDataStructureRef, Integer> {

    void deleteByStructuredObjectFund(ArrFund fund);

}
