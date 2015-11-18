package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;
import cz.tacr.elza.xmlimport.v1.vo.record.Record;

/**
 * Uzel archivní pomůcky.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "level", namespace = NamespaceInfo.NAMESPACE)
public class Level {

    /** Pozice uzlu ve své úrovni. */
    @XmlElement(required = true)
    private Integer position;

    /** Univerzální unikátní identifikátor. */
    @XmlElement(required = true)
    private String uuid;

    /** Potomci. */
    @XmlElement(required = true, name = "level")
    @XmlElementWrapper(name = "sub-level-list")
    private List<Level> subLevels;

    /** Hodnoty archivního popisu. */
    @XmlElementWrapper(name = "desc-item-list")
    @XmlElements(value = {
            @XmlElement(name = "desc-item-coordinates", type = DescItemCoordinates.class),
            @XmlElement(name = "desc-item-decimal", type = DescItemDecimal.class),
            @XmlElement(name = "desc-item-formatted-text", type = DescItemFormattedText.class),
            @XmlElement(name = "desc-item-integer", type = DescItemInteger.class),
            @XmlElement(name = "desc-item-party-ref", type = DescItemPartyRef.class),
            @XmlElement(name = "desc-item-record-ref", type = DescItemRecordRef.class),
            @XmlElement(name = "desc-item-string", type = DescItemString.class),
            @XmlElement(name = "desc-item-text", type = DescItemText.class),
            @XmlElement(name = "desc-item-unit-date", type = DescItemUnitDate.class),
            @XmlElement(name = "desc-item-unit-id", type = DescItemUnitId.class)
    })
    private List<AbstractDescItem> descItems;

    /** Vazba na rejstřík. */
    @XmlIDREF
    @XmlElement(required = true, name = "record")
    @XmlElementWrapper(name = "record-list")
    private List<Record> records;

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<Level> getSubLevels() {
        return subLevels;
    }

    public void setSubLevels(List<Level> subLevels) {
        this.subLevels = subLevels;
    }

    public List<AbstractDescItem> getDescItems() {
        return descItems;
    }

    public void setDescItems(List<AbstractDescItem> descItems) {
        this.descItems = descItems;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
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
