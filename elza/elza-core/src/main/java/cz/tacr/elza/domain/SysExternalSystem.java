package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
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
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.JOINED)
@Table
public abstract class SysExternalSystem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
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

    @Column(length = StringLength.LENGTH_50)
    private String elzaCode;

    @Column(length = StringLength.LENGTH_50)
    private String apiKeyId;

    @Column(length = StringLength.LENGTH_250)
    private String apiKeyValue;

    public static final String PK = "externalSystemId";

    public Integer getExternalSystemId() {
        return externalSystemId;
    }

    public void setExternalSystemId(final Integer externalSystemId) {
        this.externalSystemId = externalSystemId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getElzaCode() {
        return elzaCode;
    }

    public void setElzaCode(String elzaCode) {
        this.elzaCode = elzaCode;
    }

    public String getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
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
