package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.ParPartyTypeComplementType;


/**
 *  * Repozitory pro {@link ParPartyTypeComplementType}

 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 22.12.2015
 */
public interface PartyTypeComplementTypeRepository extends JpaRepository<ParPartyTypeComplementType, Integer>, Packaging<ParPartyTypeComplementType> {

}
