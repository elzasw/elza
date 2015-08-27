package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegExternalSource;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository externích zdrojů rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ExternalSourceRepository extends JpaRepository<RegExternalSource, Integer> {
}
