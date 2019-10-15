package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrFund;


/**
 * ArrDataFileRef repository
 *
 * @since 20.6.2016
 */
@Repository
public interface DataFileRefRepository extends JpaRepository<ArrDataFileRef, Integer> {

    void deleteByFileFund(ArrFund fund);

}
