package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.RegRecord;

/**
 * Repozitory pro {@link ArrDataRecordRef}
 */
@Repository
public interface DataRecordRefRepository extends JpaRepository<ArrDataRecordRef, Integer>, DataRecordRefRepositoryCustom {

    /**
     * Vazby dat na rejstříkové heslo.
     * @param recordId  id hesla
     * @return  množina odkazujících dat, může být prázdná
     */
    List<ArrDataRecordRef> findByRecord(RegRecord record);
}