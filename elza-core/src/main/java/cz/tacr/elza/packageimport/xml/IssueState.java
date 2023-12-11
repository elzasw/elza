package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Issue type (XML)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "issue-state")
public class IssueState {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "start-state")
    private boolean startState;

    @XmlElement(name = "final-state")
    private boolean finalState;

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

    public boolean isStartState() {
        return startState;
    }

    public void setStartState(boolean startState) {
        this.startState = startState;
    }

    public boolean isFinalState() {
        return finalState;
    }

    public void setFinalState(boolean finalState) {
        this.finalState = finalState;
    }
}
