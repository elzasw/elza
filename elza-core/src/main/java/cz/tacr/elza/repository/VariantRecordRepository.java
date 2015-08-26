package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegVariantRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository pro variantní rejstříková hesla.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface VariantRecordRepository extends JpaRepository<RegVariantRecord, Integer> {


}
