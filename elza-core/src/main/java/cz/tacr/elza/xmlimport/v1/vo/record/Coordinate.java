package cz.tacr.elza.xmlimport.v1.vo.record;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Souřadnice.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "coordinate", namespace = NamespaceInfo.NAMESPACE)
public class Coordinate {

    /** Pořadí. */
    @XmlAttribute(name = "position")
    private Integer position;

    /** Souřadnice. */
    @XmlElement(name = "coordinate")
    private String coordinate;

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
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
