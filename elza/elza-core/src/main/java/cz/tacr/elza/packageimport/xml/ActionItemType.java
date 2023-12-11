package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO ActionItemType.
 *
 * @author Martin Šlapa
 * @since 28.06.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "action-item-type")
public class ActionItemType {

    @XmlAttribute(name = "item-type", required = true)
    private String itemType;

    public String getItemType() {
        return itemType;
    }

    public void setItemType(final String itemType) {
        this.itemType = itemType;
    }
}
