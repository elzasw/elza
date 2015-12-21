package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RegRecord;

/**
 * Repository záznamy v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface RegRecordRepository extends JpaRepository<RegRecord, Integer>, RegRecordRepositoryCustom {

    @Query("SELECT r FROM reg_record r WHERE r.externalId = ?1 "
            + "and r.externalSource.code = ?2")
    RegRecord findRegRecordByExternalIdAndExternalSourceCode(String externalId, String externalSourceCode);
}
