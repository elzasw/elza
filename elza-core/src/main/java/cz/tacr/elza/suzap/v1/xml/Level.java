package cz.tacr.elza.suzap.v1.xml;

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

    /** Potomci. */
    @XmlElement(required = true)
    @XmlElementWrapper(name = "sub-levels")
    private List<Level> levels;

    /** Hodnoty archivního popisu. */
    @XmlElementWrapper(name = "values")
    @XmlElements(value = {
            @XmlElement(name = "value-coordinates", type = ValueCoordinates.class),
            @XmlElement(name = "value-decimal", type = ValueDecimal.class),
            @XmlElement(name = "value-formatted-text", type = ValueFormattedText.class),
            @XmlElement(name = "value-integer", type = ValueInteger.class),
            @XmlElement(name = "value-party-ref", type = ValuePartyRef.class),
            @XmlElement(name = "value-record-ref", type = ValueRecordRef.class),
            @XmlElement(name = "value-string", type = ValueString.class),
            @XmlElement(name = "value-text", type = ValueText.class),
            @XmlElement(name = "value-unit-date", type = ValueUnitDate.class),
            @XmlElement(name = "value-unit-id", type = ValueUnitId.class)
    })
    private List<AbstractValue> values;

    /** Vazba na rejstřík. */
    @XmlIDREF
    @XmlElement(required = true)
    @XmlElementWrapper(name = "records")
    private List<Record> records;

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public void setLevels(List<Level> levels) {
        this.levels = levels;
    }

    public List<AbstractValue> getValues() {
        return values;
    }

    public void setValues(List<AbstractValue> values) {
        this.values = values;
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
