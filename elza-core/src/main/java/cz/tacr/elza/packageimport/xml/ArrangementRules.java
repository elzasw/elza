package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO PackageRules.
 *
 * @since 24.10.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "arrangement-rules")
@XmlType(name = "arrangement-rules")
public class ArrangementRules {

    @XmlElement(name = "arrangement-rule", required = true)
    private List<ArrangementRule> arrangementRules;

    public List<ArrangementRule> getArrangementRules() {
        return arrangementRules;
    }

    public void setArrangementRules(final List<ArrangementRule> arrangementRules) {
        this.arrangementRules = arrangementRules;
    }
}
