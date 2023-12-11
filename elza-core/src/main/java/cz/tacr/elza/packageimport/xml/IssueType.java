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
@XmlType(name = "issue-type")
public class IssueType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "view-order", required = true)
    private Integer viewOrder;

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

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(Integer viewOrder) {
        this.viewOrder = viewOrder;
    }
}
