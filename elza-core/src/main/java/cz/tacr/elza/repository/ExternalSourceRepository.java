package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.RegExternalSource;

/**
 * Repository externích zdrojů rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ExternalSourceRepository extends JpaRepository<RegExternalSource, Integer> {

    RegExternalSource findExternalSourceByCode(String externalSourceCode);
}
