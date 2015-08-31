package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.ParPartySubtype;

/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PartySubtypeRepository extends JpaRepository<ParPartySubtype, Integer> {

}
