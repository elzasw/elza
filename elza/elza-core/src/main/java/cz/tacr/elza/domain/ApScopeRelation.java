package cz.tacr.elza.domain;

import jakarta.persistence.*;

@Entity(name = "ap_scope_relation")
@Table
public class ApScopeRelation {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer scopeRelationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scopeId", nullable = false)
    private ApScope scope;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "connectedScopeId", nullable = false)
    private ApScope connectedScope;

    public Integer getScopeRelationId() {
        return scopeRelationId;
    }

    public void setScopeRelationId(Integer scopeRelationId) {
        this.scopeRelationId = scopeRelationId;
    }

    public ApScope getScope() {
        return scope;
    }

    public void setScope(ApScope scope) {
        this.scope = scope;
    }

    public ApScope getConnectedScope() {
        return connectedScope;
    }

    public void setConnectedScope(ApScope connectedScope) {
        this.connectedScope = connectedScope;
    }
}
