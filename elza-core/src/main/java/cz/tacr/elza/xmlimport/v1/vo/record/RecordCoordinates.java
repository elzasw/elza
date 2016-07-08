package cz.tacr.elza.xmlimport.v1.vo.record;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Souřadnice rejstříkového hesla.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "record-coordinates", namespace = NamespaceInfo.NAMESPACE)
public class RecordCoordinates {

    /** Typ oblasti. //TODO  výčet point|line|geoarea|polygon*/
    @XmlAttribute(name = "area-type")
    private String areaType;

    /** Použitý systém souřadnic. */
    @XmlAttribute(name = "system")
    private String system;

    /** Poznámka. */
    @XmlElement(name = "note")
    private String note;

    /** Seznam souřadnic.*/
    @XmlElement(name = "coordinate")
    @XmlElementWrapper(name = "coordinate-list")
    private List<Coordinate> coordinates;

    public String getAreaType() {
        return areaType;
    }

    public void setAreaType(String areaType) {
        this.areaType = areaType;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Coordinate> coordinates) {
        this.coordinates = coordinates;
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
