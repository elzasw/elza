package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegRegisterType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository pro typ záznamu rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegisterTypeRepository extends JpaRepository<RegRegisterType, Integer> {
}
