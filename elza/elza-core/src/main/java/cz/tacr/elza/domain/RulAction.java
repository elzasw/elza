package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Entity(name = "rul_action")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulAction {

    public static final String FILE_EXTENSION = ".yaml";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer actionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer packageId;

    @Column(length = 250, nullable = false)
    private String filename;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer ruleSetId;

    /**
     * @return identifikátor entity
     */
    public Integer getActionId() {
        return actionId;
    }

    /**
     * @param actionId identifikátor entity
     */
    public void setActionId(final Integer actionId) {
        this.actionId = actionId;
    }

    /**
     * @return balíček
     */
    public RulPackage getPackage() {
        return rulPackage;
    }

    /**
     * @param rulPackage balíček
     */
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
        this.packageId = rulPackage != null ? rulPackage.getPackageId() : null;
    }

    public Integer getPackageId() {
        return packageId;
    }

    /**
     * @return název souboru
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename název souboru
     */
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * @return pravidla
     */
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    /**
     * @param ruleSet pravidla
     */
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        this.ruleSetId = ruleSet != null ? ruleSet.getRuleSetId() : null;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    /**
     * @return Code from filename (without extension).
     */
    @Transient
    public String getCode() {
        int len = filename.length() - FILE_EXTENSION.length();
        return filename.substring(0, len);
    }

    public static String getFileNameFromCode(String code) {
        return Validate.notEmpty(code) + FILE_EXTENSION;
    }
}
