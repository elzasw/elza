package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParRelation;


/**
 * Repozitory pro {@link ParRelation}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 04.01.2016
 */
@Repository
public interface RelationRepository extends JpaRepository<ParRelation, Integer> {

    /**
     * Najde vztahy osoby.
     *
     * @param party osoba
     * @return vztahy osoby
     */
    List<ParRelation> findByParty(ParParty party);
}
