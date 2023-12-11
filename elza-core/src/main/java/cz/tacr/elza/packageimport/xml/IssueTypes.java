package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * List of available issue types
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "issue-types")
@XmlType(name = "issue-types")
public class IssueTypes {

    @XmlElement(name = "issue-type", required = true)
    private List<IssueType> issueTypes;

    public List<IssueType> getIssueTypes() {
        return issueTypes;
    }

    public void setIssueTypes(List<IssueType> issueTypes) {
        this.issueTypes = issueTypes;
    }
}
