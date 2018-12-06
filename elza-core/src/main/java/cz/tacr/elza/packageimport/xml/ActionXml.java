package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO PackageAction.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "package-action")
public class ActionXml {

    @XmlAttribute(name = "filename", required = true)
    private String filename;

    @XmlElement(name = "action-item-type")
    @XmlElementWrapper(name = "action-item-types")
    private List<ActionItemType> actionItemTypes;

    @XmlElement(name = "action-recommended")
    @XmlElementWrapper(name = "action-recommendeds")
    private List<ActionRecommended> actionRecommendeds;

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public List<ActionItemType> getActionItemTypes() {
        return actionItemTypes;
    }

    public void setActionItemTypes(final List<ActionItemType> actionItemTypes) {
        this.actionItemTypes = actionItemTypes;
    }

    public List<ActionRecommended> getActionRecommendeds() {
        return actionRecommendeds;
    }

    public void setActionRecommendeds(final List<ActionRecommended> actionRecommendeds) {
        this.actionRecommendeds = actionRecommendeds;
    }
}
