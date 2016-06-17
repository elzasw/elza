package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;
import java.util.List;


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
