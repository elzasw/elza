package cz.tacr.elza.packageimport.xml;

import cz.tacr.elza.domain.RulItemTypeSpecAssign;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

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
     * Převod DAO na VO typů atributu.
     *
     * @param rulItemTypeSpecAssign DAO
     */
    public static ItemTypeAssign fromEntity(RulItemTypeSpecAssign rulItemTypeSpecAssign) {
        ItemTypeAssign itemType = new ItemTypeAssign();
        itemType.setCode(rulItemTypeSpecAssign.getItemType().getCode());
        return itemType;
    }
}
