package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository pro události.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface PartyEventRepository extends JpaRepository<ParEvent, Integer> {


}
