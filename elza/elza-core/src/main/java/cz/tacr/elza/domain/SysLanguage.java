package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "sys_language")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Table
public class SysLanguage {

    public static final String FIELD_CODE = "code";
    public static final String FIELD_NAME = "name";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer languageId;

    @Column(length = 3, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

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
}
