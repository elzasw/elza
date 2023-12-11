package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO ArrangementExtensions.
 *
 * @since 17.10.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "arrangement-extensions")
@XmlType(name = "arrangement-extensions")
public class ArrangementExtensions {

    @XmlElement(name = "arrangement-extension", required = true)
    private List<ArrangementExtension> arrangementExtensions;

    public List<ArrangementExtension> getArrangementExtensions() {
        return arrangementExtensions;
    }

    public void setArrangementExtensions(final List<ArrangementExtension> arrangementExtensions) {
        this.arrangementExtensions = arrangementExtensions;
    }
}
