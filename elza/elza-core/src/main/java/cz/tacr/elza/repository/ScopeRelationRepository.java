package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApScopeRelation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;


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

    /**
     * Získání seznamu návazaních Scope podle seznamu Scope
     * 
     * @param scopes
     * @return Set<ApScope>
     */
    @Query("SELECT s.connectedScope FROM ap_scope_relation s WHERE s.scope in (?1)")
    Set<ApScope> findConnectedScopeByScope(Collection<ApScope> scopes);

    /**
     * Získání seznamu Id návazaních Scope podle seznamu Scope Id
     * 
     * @param scopeIds
     * @return Set<Integer>
     */
    @Query("SELECT s.connectedScope.scopeId FROM ap_scope_relation s WHERE s.scope.scopeId in (?1)")
    Set<Integer> findConnectedScopeIdsByScopeIds(Collection<Integer> scopeIds);

}
