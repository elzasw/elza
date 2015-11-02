package cz.tacr.elza.suzap.v1.xml;

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
 * Archivní pomůcka.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "findinig-aid", namespace = NamespaceInfo.NAMESPACE)
@XmlRootElement(name = "findinig-aid", namespace = NamespaceInfo.NAMESPACE)
public class FindingAid {

    /** Název archivní pomůcky. */
    @XmlElement(required = true)
    private String name;

    /** Kořenový uzel. */
    @XmlElement(required = true)
    private Level rootLevel;

    /** Seznam rejstříkových hesel. */
    @XmlElement(name = "record")
    @XmlElementWrapper(name = "record-list")
    private List<Record> records;

    /** Seznam osob. */
    @XmlElement(name = "party")
    @XmlElementWrapper(name = "party-list")
    private List<Party> parties;

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
