package cz.tacr.elza.packageimport.xml;

import cz.tacr.elza.domain.RulExtensionRule;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO ExtensionRule {@link RulExtensionRule}.
 *
 * @since 17.10.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "extension-rule")
public class ExtensionRule {

    /**
     * Název souboru.
     */
    @XmlAttribute(name = "filename", required = true)
    private String filename;

    /**
     * Kód definice rozšíření.
     */
    @XmlAttribute(name = "arrangement-extension", required = true)
    private String arrangementExtension;

    /**
     * Typ pravidla.
     */
    @XmlElement(name = "rule-type", required = true)
    private RulExtensionRule.RuleType ruleType;

    /**
     * Priorita.
     */
    @XmlElement(name = "priority", required = true)
    private Integer priority;

    /**
     * Kompatibilní balíček.
     */
    @XmlAttribute(name="compatibility-rul-package")
    private Integer compatibilityRulPackage;

    /**
     * Stav.
     */
    @XmlElement(name="condition")
    private String condition;

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getArrangementExtension() {
        return arrangementExtension;
    }

    public void setArrangementExtension(final String arrangementExtension) {
        this.arrangementExtension = arrangementExtension;
    }

    public RulExtensionRule.RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(final RulExtensionRule.RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public Integer getCompatibilityRulPackage() {
        return compatibilityRulPackage;
    }

    public void setCompatibilityRulPackage(Integer compatibilityRulPackage) {
        this.compatibilityRulPackage = compatibilityRulPackage;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
