package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

@Entity(name = "ui_visible_policy")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UIVisiblePolicy implements cz.tacr.elza.api.UIVisiblePolicy<ArrNode, RulPolicyType> {

    @Id
    @GeneratedValue
    private Integer visiblePolicyId;

    @Column(nullable = false)
    private Boolean visible;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPolicyType.class)
    @JoinColumn(name = "policyTypeId", nullable = false)
    private RulPolicyType policyType;

    @Override
    public Integer getVisiblePolicyId() {
        return visiblePolicyId;
    }

    @Override
    public void setVisiblePolicyId(final Integer visiblePolicyId) {
        this.visiblePolicyId = visiblePolicyId;
    }

    @Override
    public Boolean getVisible() {
        return visible;
    }

    @Override
    public void setVisible(final Boolean visible) {
        this.visible = visible;
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    @Override
    public RulPolicyType getPolicyType() {
        return policyType;
    }

    @Override
    public void setPolicyType(final RulPolicyType policyType) {
        this.policyType = policyType;
    }
}
