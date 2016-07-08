package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Archivní pomůcka.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fund", namespace = NamespaceInfo.NAMESPACE)
public class Fund {

    /** Název archivní pomůcky. */
    @XmlElement(name = "name", required = true)
    private String name;

    /** Kořenový uzel. */
    @XmlElement(name = "root-level", required = true)
    private Level rootLevel;

    /** Kód typu výstupu. */
    @XmlAttribute(name = "arr-type-code")
    private String arrangementTypeCode;

    /** Kód pravidel. */
    @XmlAttribute(name = "rule-set-code")
    private String ruleSetCode;

    /** Kód instituce. */
    @XmlAttribute(name = "institution-code", required = true)
    private String institutionCode;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Level getRootLevel() {
        return rootLevel;
    }

    public void setRootLevel(Level rootLevel) {
        this.rootLevel = rootLevel;
    }

    public String getArrangementTypeCode() {
        return arrangementTypeCode;
    }

    public void setArrangementTypeCode(String arrangementTypeCode) {
        this.arrangementTypeCode = arrangementTypeCode;
    }

    public String getRuleSetCode() {
        return ruleSetCode;
    }

    public void setRuleSetCode(String ruleSetCode) {
        this.ruleSetCode = ruleSetCode;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
