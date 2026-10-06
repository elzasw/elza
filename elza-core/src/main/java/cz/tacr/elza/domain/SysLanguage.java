package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "sys_language")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Table
public class SysLanguage {

    public static final String FIELD_CODE = "code";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_TAG = "tag";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer languageId;

    @Column(length = 3, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    /**
     * BCP 47 tag ({@code cs}, {@code en}); the key used by translation files, Accept-Language and
     * the client.
     */
    @Column(length = 10, nullable = false, unique = true)
    private String tag;

    /** The UI can be used in this language. */
    @Column(nullable = false)
    private Boolean uiEnabled;

    /** The language can be chosen as the language of an entity scope. */
    @Column(nullable = false)
    private Boolean scopeEnabled;

    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Boolean getUiEnabled() {
        return uiEnabled;
    }

    public void setUiEnabled(Boolean uiEnabled) {
        this.uiEnabled = uiEnabled;
    }

    public Boolean getScopeEnabled() {
        return scopeEnabled;
    }

    public void setScopeEnabled(Boolean scopeEnabled) {
        this.scopeEnabled = scopeEnabled;
    }
}
