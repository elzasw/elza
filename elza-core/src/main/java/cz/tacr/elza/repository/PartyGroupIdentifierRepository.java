package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;


/**
 * Repozitory pro {@link ParPartyGroupIdentifier}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 04.01.2016
 */
@Repository
public interface PartyGroupIdentifierRepository extends JpaRepository<ParPartyGroupIdentifier, Integer> {

    /**
     * Najde seznam identifikací.
     *
     * @param party group party
     * @return seznam identifikací group party
     */
    List<ParPartyGroupIdentifier> findByPartyGroup(ParPartyGroup party);
}
