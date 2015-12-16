package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO ArrangementType.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "arrangement-type")
public class ArrangementType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "rule-set", required = true)
    private String ruleSet;

    @XmlElement(name = "name", required = true)
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getRuleSet() {
        return ruleSet;
    }

    public void setRuleSet(final String ruleSet) {
        this.ruleSet = ruleSet;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
