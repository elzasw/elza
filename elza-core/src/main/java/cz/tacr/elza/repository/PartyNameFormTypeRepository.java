package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParPartyNameFormType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */

public interface PartyNameFormTypeRepository extends JpaRepository<ParPartyNameFormType, Integer> {

}
