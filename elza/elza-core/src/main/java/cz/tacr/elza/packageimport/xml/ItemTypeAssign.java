package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import cz.tacr.elza.domain.RulItemTypeSpecAssign;

/**
 * VO ItemTypeAssign from XML
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item-type-assign")
public class ItemTypeAssign {
    @XmlAttribute(name = "code", required = true)
    private String code;

    /**
     * Code of the specification of the item type after which this specification is placed.
     * Optional; without it the specification keeps its default position.
     */
    @XmlAttribute(name = "view-after")
    private String viewAfter;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getViewAfter() {
        return viewAfter;
    }

    public void setViewAfter(final String viewAfter) {
        this.viewAfter = viewAfter;
    }

    /**
     * Převod z DB na XML typ
     *
     * @param assignment
     *            assignment of the specification to an item type
     */
    public static ItemTypeAssign fromEntity(RulItemTypeSpecAssign assignment) {
        ItemTypeAssign itemType = new ItemTypeAssign();
        itemType.setCode(assignment.getItemType().getCode());
        itemType.setViewAfter(assignment.getViewAfterSpecCode());
        return itemType;
    }
}
