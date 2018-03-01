package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ApRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataRecordRef;

/**
 * Repozitory pro {@link ArrDataRecordRef}
 */
@Repository
public interface DataRecordRefRepository extends JpaRepository<ArrDataRecordRef, Integer>, DataRecordRefRepositoryCustom {

    /**
     * Vazby dat na rejstříkové heslo.
     * @param record rejstřík
     * @return  množina odkazujících dat, může být prázdná
     */
    List<ArrDataRecordRef> findByRecord(ApRecord record);

    /**
     * Počet vazeb na rejstříkové heslo.
     * @param record rejstřík
     * @return počet odkazujících dat
     */
    long countAllByRecord(ApRecord record);
}
