package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;
import java.util.List;


/**
 * ItemSpec.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item-spec")
public class ItemSpec {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "item-type", required = true)
    private String itemType;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "shortcut", required = true)
    private String shortcut;

    @XmlElement(name = "description", required = true)
    private String description;

    @XmlElement(name = "view-order", required = true)
    private Integer viewOrder;

    @XmlElement(name = "desc-item-spec-register")
    @XmlElementWrapper(name = "desc-item-spec-registers")
    private List<DescItemSpecRegister> descItemSpecRegisters;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(final String itemType) {
        this.itemType = itemType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(final String shortcut) {
        this.shortcut = shortcut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    public List<DescItemSpecRegister> getDescItemSpecRegisters() {
        return descItemSpecRegisters;
    }

    public void setDescItemSpecRegisters(final List<DescItemSpecRegister> descItemSpecRegisters) {
        this.descItemSpecRegisters = descItemSpecRegisters;
    }
}
