package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * VO ItemTypeAssign from XML
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item-type-assign")
public class ItemTypeAssign {
    @XmlAttribute(name = "code", required = true)
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * Převod z DB na XML typ
     *
     * @param itemTypeCode
     *            Item type
     */
    public static ItemTypeAssign fromEntity(String itemTypeCode) {
        ItemTypeAssign itemType = new ItemTypeAssign();
        itemType.setCode(itemTypeCode);
        return itemType;
    }
}
