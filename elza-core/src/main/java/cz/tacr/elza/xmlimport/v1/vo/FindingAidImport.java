package cz.tacr.elza.xmlimport.v1.vo;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Kořenový prvek entit pro import archivní pomůcky.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "findinig-aid-import", namespace = NamespaceInfo.NAMESPACE)
@XmlRootElement(name = "findinig-aid-import", namespace = NamespaceInfo.NAMESPACE)
public class FindingAidImport {

    /** Archivní pomůcka. */
    @XmlElement
    private FindingAid findingAid;

    /** Seznam rejstříkových hesel. */
    @XmlElement(name = "record")
    @XmlElementWrapper(name = "record-list")
    private List<Record> records;

    /** Seznam osob. */
    @XmlElement(name = "party")
    @XmlElementWrapper(name = "party-list")
    private List<Party> parties;

    public FindingAid getFindingAid() {
        return findingAid;
    }

    public void setFindingAid(FindingAid findingAid) {
        this.findingAid = findingAid;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }

    public List<Party> getParties() {
        return parties;
    }

    public void setParties(List<Party> parties) {
        this.parties = parties;
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
