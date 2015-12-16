package cz.tacr.elza.packageimport.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO ArrangementTypes.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "arrangement-types")
@XmlType(name = "arrangement-types")
public class ArrangementTypes {

    @XmlElement(name = "arrangement-type", required = true)
    private List<ArrangementType> arrangementTypes;

    public List<ArrangementType> getArrangementTypes() {
        return arrangementTypes;
    }

    public void setArrangementTypes(final List<ArrangementType> arrangementTypes) {
        this.arrangementTypes = arrangementTypes;
    }
}
