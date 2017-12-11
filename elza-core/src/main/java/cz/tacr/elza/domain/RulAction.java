package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

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
    private Integer actionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(length = 250, nullable = false)
    private String filename;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

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
    }

    /**
     * @return Code from filename (without extension).
     */
    @Transient
    public String getCode() {
        int len = filename.length() - FILE_EXTENSION.length();
        return filename.substring(0, len);
    }
}
