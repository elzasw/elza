package cz.tacr.elza.packageimport.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * List of available issue states
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "issue-states")
@XmlType(name = "issue-states")
public class IssueStates {

    @XmlElement(name = "issue-state", required = true)
    private List<IssueState> issueStates;

    public List<IssueState> getIssueStates() {
        return issueStates;
    }

    public void setIssueStates(List<IssueState> issueStates) {
        this.issueStates = issueStates;
    }
}
