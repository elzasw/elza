package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO ItemSpecs.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "item-specs")
@XmlType(name = "item-specs")
public class ItemSpecs {

    @XmlElement(name = "item-spec", required = true)
    private List<ItemSpec> itemSpecs;

    public List<ItemSpec> getItemSpecs() {
        return itemSpecs;
    }

    public void setItemSpecs(final List<ItemSpec> itemSpecs) {
        this.itemSpecs = itemSpecs;
    }
}
