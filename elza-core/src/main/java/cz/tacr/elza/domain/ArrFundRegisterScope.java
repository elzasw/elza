package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Napojení třídy rejstříku na FA.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
@Entity(name = "arr_fund_register_scope")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFundRegisterScope {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer fundRegisterScopeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scopeId", nullable = false)
    private ApScope scope;

    public Integer getFundRegisterScopeId() {
        return fundRegisterScopeId;
    }

    public void setFundRegisterScopeId(final Integer fundRegisterScopeId) {
        this.fundRegisterScopeId = fundRegisterScopeId;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    public ApScope getScope() {
        return scope;
    }

    public void setScope(final ApScope scope) {
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

        cz.tacr.elza.domain.ArrFundRegisterScope that = (cz.tacr.elza.domain.ArrFundRegisterScope) o;

        return new EqualsBuilder()
                .append(fundRegisterScopeId, that.fundRegisterScopeId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(fundRegisterScopeId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ArrFundRegisterScope{" +
                "fundRegisterScopeId=" + fundRegisterScopeId +
                ", fund=" + fund +
                ", scope=" + scope +
                '}';
    }
}
