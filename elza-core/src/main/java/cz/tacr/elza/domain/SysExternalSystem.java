package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Číselník externích systémů.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2016
 */
@Entity(name = "sys_external_system")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.JOINED)
@Table
public abstract class SysExternalSystem implements cz.tacr.elza.api.SysExternalSystem {

    @Id
    @GeneratedValue
    private Integer externalSystemId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(length = StringLength.LENGTH_1000)
    private String url;

    @Column(length = StringLength.LENGTH_50)
    private String username;

    @Column(length = StringLength.LENGTH_50)
    private String password;

    @Override
    public Integer getExternalSystemId() {
        return externalSystemId;
    }

    @Override
    public void setExternalSystemId(final Integer externalSystemId) {
        this.externalSystemId = externalSystemId;
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
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(final String url) {
        this.url = url;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(final String password) {
        this.password = password;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof SysExternalSystem)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        SysExternalSystem other = (SysExternalSystem) obj;

        return new EqualsBuilder().append(externalSystemId, other.getExternalSystemId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(externalSystemId).toHashCode();
    }

    @Override
    public String toString() {
        return "SysExternalSystem pk=" + externalSystemId;
    }
}
