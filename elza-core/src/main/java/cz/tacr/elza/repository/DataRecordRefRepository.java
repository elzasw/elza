package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<ArrDataRecordRef> findByRecord(ApAccessPoint record);

    /**
     * Počet vazeb na rejstříkové heslo.
     * @param record rejstřík
     * @return počet odkazujících dat
     */
    long countAllByRecord(ApAccessPoint record);

    @Query("SELECT drr FROM arr_data_record_ref drr LEFT JOIN drr.binding WHERE drr.binding IN :bindings")
    List<ArrDataRecordRef> findByBindingIn(@Param("bindings") List<ApBinding> bindings);

    @Query("UPDATE arr_data_record_ref drr SET drr.binding = NULL WHERE drr.binding = :binding")
    @Modifying
    void disconnectBinding(@Param("binding") ApBinding binding);
}
