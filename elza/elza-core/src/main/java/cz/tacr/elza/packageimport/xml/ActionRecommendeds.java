package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO ActionRecommendeds.
 *
 * @author Martin Šlapa
 * @since 28.06.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "action-recommendeds")
@XmlType(name = "action-recommendeds")
public class ActionRecommendeds {

    @XmlElement(name = "action-recommended", required = true)
    private List<ActionRecommended> actionRecommendeds;

    public List<ActionRecommended> getActionRecommendeds() {
        return actionRecommendeds;
    }

    public void setActionRecommendeds(final List<ActionRecommended> actionRecommendeds) {
        this.actionRecommendeds = actionRecommendeds;
    }
}
