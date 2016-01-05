package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDataRecordRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link ArrDataRecordRef}
 */
@Repository
public interface DataRecordRefRepository extends JpaRepository<ArrDataRecordRef, Integer> {

    /**
     * Vazby dat na rejstříkové heslo.
     * @param recordId  id hesla
     * @return  množina odkazujících dat, může být prázdná
     */
    List<ArrDataRecordRef> findByRecordId(Integer recordId);

}
