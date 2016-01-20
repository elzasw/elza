package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface PartyPersonRepository extends JpaRepository<ParPerson, Integer> {

}
