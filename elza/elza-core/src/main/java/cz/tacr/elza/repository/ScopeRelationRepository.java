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

    @Query("SELECT COUNT(p) FROM par_party p " +
            "JOIN p.accessPoint apFrom " +
            "JOIN ap_state stateFrom ON stateFrom.accessPoint = apFrom " +
            "JOIN p.relations rel " +
            "JOIN rel.relationEntities relEnt " +
            "JOIN relEnt.accessPoint apTo " +
            "JOIN ap_state stateTo ON stateTo.accessPoint = apTo " +
            "WHERE stateFrom.deleteChange IS NULL AND stateFrom.scope = :scopeFrom AND stateTo.deleteChange IS NULL AND stateTo.scope = :scopeTo")
    Long countExistsRelations(@Param("scopeFrom") ApScope scopeFrom,
                              @Param("scopeTo") ApScope scopeTo);
}
