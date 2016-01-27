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


/**
 * Třída rejstříku.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
@Entity(name = "reg_scope")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"code"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegScope implements cz.tacr.elza.api.RegScope {

    public static final String SCOPE_ID = "scopeId";

    @Id
    @GeneratedValue
    private Integer scopeId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Override
    public Integer getScopeId() {
        return scopeId;
    }

    @Override
    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RegScope regScope = (RegScope) o;

        return new EqualsBuilder()
                .append(scopeId, regScope.scopeId)
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
        return "RegScope{" +
                "scopeId=" + scopeId +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
