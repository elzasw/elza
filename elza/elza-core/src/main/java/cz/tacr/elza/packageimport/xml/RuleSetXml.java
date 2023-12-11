package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * RuleSet in XML
 *
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rule-set")
public class RuleSetXml {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "rule-type", required = true)
    private String ruleType;

    @XmlElement(name = "rule-item-type-filter")
    private String ruleItemTypeFilter;

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

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleItemTypeFilter() {
        return ruleItemTypeFilter;
    }

    public void setRuleItemTypeFilter(String ruleItemTypeFilter) {
        this.ruleItemTypeFilter = ruleItemTypeFilter;
    }
}
