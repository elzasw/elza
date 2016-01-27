package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Napojení třídy rejstříku na FA.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
@Entity(name = "arr_fa_register_scope")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFaRegisterScope implements cz.tacr.elza.api.ArrFaRegisterScope<ArrFindingAid, RegScope> {

    @Id
    @GeneratedValue
    private Integer faRegisterScopeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFindingAid.class)
    @JoinColumn(name = "findingAidId", nullable = false)
    private ArrFindingAid findingAid;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegScope.class)
    @JoinColumn(name = "scopeId", nullable = false)
    private RegScope scope;

    public Integer getFaRegisterScopeId() {
        return faRegisterScopeId;
    }

    public void setFaRegisterScopeId(final Integer faRegisterScopeId) {
        this.faRegisterScopeId = faRegisterScopeId;
    }

    public ArrFindingAid getFindingAid() {
        return findingAid;
    }

    public void setFindingAid(final ArrFindingAid findingAid) {
        this.findingAid = findingAid;
    }

    public RegScope getScope() {
        return scope;
    }

    public void setScope(final RegScope scope) {
        this.scope = scope;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArrFaRegisterScope that = (ArrFaRegisterScope) o;

        return new EqualsBuilder()
                .append(faRegisterScopeId, that.faRegisterScopeId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(faRegisterScopeId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ArrFaRegisterScope{" +
                "faRegisterScopeId=" + faRegisterScopeId +
                ", findingAid=" + findingAid +
                ", scope=" + scope +
                '}';
    }
}
