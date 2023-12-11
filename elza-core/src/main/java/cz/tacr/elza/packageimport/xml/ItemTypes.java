package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO ItemTypes.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "item-types")
@XmlType(name = "item-types")
public class ItemTypes {

    @XmlElement(name = "item-type", required = true)
    private List<ItemType> itemTypes;

    public List<ItemType> getItemTypes() {
        return itemTypes;
    }

    public void setItemTypes(final List<ItemType> itemTypes) {
        this.itemTypes = itemTypes;
    }
}
