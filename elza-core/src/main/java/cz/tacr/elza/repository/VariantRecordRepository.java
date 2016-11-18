package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegVariantRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository pro variantní rejstříková hesla.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface VariantRecordRepository extends ElzaJpaRepository<RegVariantRecord, Integer> {

    /**
     * @param recordId  id záznamu rejtříku
     * @return  záznamy patřící danému záznamu v rejstříku
     */
    @Query("SELECT vr FROM reg_variant_record vr JOIN vr.regRecord r WHERE r.recordId = ?1 ORDER BY vr.variantRecordId")
    List<RegVariantRecord> findByRegRecordId(Integer recordId);

}
