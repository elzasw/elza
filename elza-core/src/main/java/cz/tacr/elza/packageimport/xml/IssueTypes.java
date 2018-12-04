package cz.tacr.elza.packageimport.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
