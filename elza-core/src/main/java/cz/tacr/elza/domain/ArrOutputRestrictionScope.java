package cz.tacr.elza.domain;

import jakarta.persistence.*;

@Entity(name = "arr_output_restriction_scope")
public class ArrOutputRestrictionScope {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer restrictionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutput.class)
    @JoinColumn(name = "outputId", nullable = false)
    private ArrOutput output;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scopeId", nullable = false)
    private ApScope scope;

    public Integer getRestrictionId() {
        return restrictionId;
    }

    public void setRestrictionId(Integer restrictionId) {
        this.restrictionId = restrictionId;
    }

    public ArrOutput getOutput() {
        return output;
    }

    public void setOutput(ArrOutput output) {
        this.output = output;
    }

    public ApScope getScope() {
        return scope;
    }

    public void setScope(ApScope scope) {
        this.scope = scope;
    }
}
