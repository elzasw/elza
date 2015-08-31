package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository záznamy v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface RegRecordRepository extends JpaRepository<RegRecord, Integer>, RegRecordRepositoryCustom {

}
