package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.api.interfaces.IApScope;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Třída rejstříku.
 */
@Entity(name = "ap_scope")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
public class ApScope implements IApScope {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer scopeId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SysLanguage.class)
    @JoinColumn(name = "languageId")
    private SysLanguage language;
    
    @Column(insertable = false, updatable = false)
    private Integer languageId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId")
    private RulRuleSet rulRuleSet;

    @Column(insertable = false, updatable = false)
    private Integer ruleSetId;

    @Override
    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(Integer scopeId) {
        this.scopeId = scopeId;
    }

    /**
     * @return Kód třídy rejstříku.
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code
     *            Kód třídy rejstříku.
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return Název třídy rejstříku.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            Název třídy rejstříku.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return @param language 3-místný kód jazyka, podle ISO 639-2.
     */
    public SysLanguage getLanguage() {
        return language;
    }

    /**
     * @param language
     *            3-místný kód jazyka, podle ISO 639-2.
     */
    public void setLanguage(SysLanguage language) {
        this.language = language;
        this.languageId = language != null ? language.getLanguageId() : null;
    }
    
    public Integer getLanguageId() {
        return languageId;
    }

    public RulRuleSet getRulRuleSet() {
        return rulRuleSet;
    }

    public void setRulRuleSet(RulRuleSet rulRuleSet) {
        this.rulRuleSet = rulRuleSet;
        this.ruleSetId = rulRuleSet != null ? rulRuleSet.getRuleSetId() : null;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    @Override
    public String toString() {
        return "ApScope [scopeId=" + scopeId + ", code=" + code + ", name=" + name + "]";
    }
}
