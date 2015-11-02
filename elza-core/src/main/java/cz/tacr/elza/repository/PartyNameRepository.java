package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.ParPartyName;

/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */

public interface PartyNameRepository extends JpaRepository<ParPartyName, Integer>, PartyNameCustomRepository {

}
