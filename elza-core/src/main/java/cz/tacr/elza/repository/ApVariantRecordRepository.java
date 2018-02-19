package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.ApVariantRecord;

/**
 * Repository pro variantní rejstříková hesla.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ApVariantRecordRepository extends ElzaJpaRepository<ApVariantRecord, Integer> {

    /**
     * @param recordId  id záznamu rejtříku
     * @return  záznamy patřící danému záznamu v rejstříku
     */
    @Query("SELECT vr FROM ap_variant_record vr JOIN vr.apRecord r WHERE r.recordId = ?1 ORDER BY vr.variantRecordId")
    List<ApVariantRecord> findByApRecordId(Integer recordId);

    @Modifying
    int deleteByApRecordIn(Collection<ApRecord> records);
}
