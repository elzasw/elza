package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO ActionItemTypes.
 *
 * @author Martin Šlapa
 * @since 28.06.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "action-item-types")
@XmlType(name = "action-item-types")
public class ActionItemTypes {

    @XmlElement(name = "action-item-type", required = true)
    private List<ActionItemType> actionItemType;

    public List<ActionItemType> getActionItemType() {
        return actionItemType;
    }

    public void setActionItemType(final List<ActionItemType> actionItemType) {
        this.actionItemType = actionItemType;
    }
}
