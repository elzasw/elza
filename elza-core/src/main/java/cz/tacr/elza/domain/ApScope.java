package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.interfaces.IApScope;


/**
 * Třída rejstříku.
 *
 */
@Entity(name = "ap_scope")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"code"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ApScope implements IApScope {

    public static final String SCOPE_ID = "scopeId";

    @Id
    @GeneratedValue
    private Integer scopeId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    /**
     * @return Kód třídy rejstříku.
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code Kód třídy rejstříku.
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * @return Název třídy rejstříku.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Název třídy rejstříku.
     */
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        ApScope apScope = (ApScope) o;

        return new EqualsBuilder()
                .append(scopeId, apScope.getScopeId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(scopeId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ApScope{" +
                "scopeId=" + scopeId +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public ApScope getApScope() {
        return this;
    }
}
