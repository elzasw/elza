package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RegRecord;


/**
 * Repository záznamy v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface RegRecordRepository extends JpaRepository<RegRecord, Integer>, RegRecordRepositoryCustom {

    /**
     * Najde potomky rejstříkového hesla.
     *
     * @param parentRecord rodič
     * @return seznam potomků
     */
    List<RegRecord> findByParentRecord(RegRecord parentRecord);
}
