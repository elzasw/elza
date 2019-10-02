package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrFund;


/**
 * @since 16.11.2017
 */
@Repository
public interface DataStructureRefRepository extends JpaRepository<ArrDataStructureRef, Integer> {

    @Modifying
    @Query("DELETE FROM arr_data_structure_ref sr WHERE sr.structuredObject IN (SELECT s FROM arr_structured_object s WHERE s.fund = ?1)")
    void deleteByStructuredObjectFund(ArrFund fund);

}
