package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApScopeRelation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Repository pro {@link ApScopeRelation}.
 *
 * @author Martin Berka [<a href="mailto:martin.berka@marbes.cz">martin.berka@marbes.cz</a>]
 * @since 03.07.2019
 */
@Repository
public interface ScopeRelationRepository extends ElzaJpaRepository<ApScopeRelation, Integer> {

    ApScopeRelation findByScopeAndConnectedScope(ApScope scope, ApScope connectedScope);

    List<ApScopeRelation> findByConnectedScope(ApScope connectedScope);

}
