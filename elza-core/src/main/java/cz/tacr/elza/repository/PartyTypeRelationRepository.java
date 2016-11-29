package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParRelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParPartyTypeRelation;


/**
 * * Repozitory pro {@link PartyTypeRelationRepository}
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@Repository
public interface PartyTypeRelationRepository extends JpaRepository<ParPartyTypeRelation, Integer>, Packaging<ParPartyTypeRelation> {

    void deleteByRelationType(ParRelationType parRelationType);
}
