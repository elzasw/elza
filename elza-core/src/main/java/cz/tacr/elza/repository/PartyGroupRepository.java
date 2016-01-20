package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParPartyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository pro korporace či skupiny.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface PartyGroupRepository extends JpaRepository<ParPartyGroup, Integer> {


}
