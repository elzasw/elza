package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Entity classes used by an entity rule set: {@code rul_rule_set/<RS>/rul_ap_type.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ap-types")
@XmlType(name = "rule-set-ap-types")
public class RuleSetApTypes {

    @XmlElement(name = "ap-type", required = true)
    private List<Member> apTypes;

    public List<Member> getApTypes() {
        return apTypes;
    }

    public void setApTypes(final List<Member> apTypes) {
        this.apTypes = apTypes;
    }

    /**
     * {@code <ap-type code="PERSON" assignable="false"/>}
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "rule-set-ap-type")
    public static class Member {

        @XmlAttribute(name = "code", required = true)
        private String code;

        /** Omitted: the opposite of read-only of the class. */
        @XmlAttribute(name = "assignable")
        private Boolean assignable;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public Boolean getAssignable() {
            return assignable;
        }

        public void setAssignable(final Boolean assignable) {
            this.assignable = assignable;
        }
    }
}
