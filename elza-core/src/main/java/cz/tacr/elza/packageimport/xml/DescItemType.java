package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO DescItemType.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "desc-item-type")
public class DescItemType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "data-type", required = true)
    private String dataType;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "shortcut", required = true)
    private String shortcut;

    @XmlElement(name = "description", required = true)
    private String description;

    @XmlElement(name = "is-value-unique", required = true)
    private Boolean isValueUnique;

    @XmlElement(name = "can-be-ordered", required = true)
    private Boolean canBeOrdered;

    @XmlElement(name = "use-specification", required = true)
    private Boolean useSpecification;

    @XmlElement(name = "view-order", required = true)
    private Integer viewOrder;

    @XmlElement(name = "fa-only", required = true)
    private Boolean faOnly;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(final String dataType) {
        this.dataType = dataType;
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

    public Boolean getIsValueUnique() {
        return isValueUnique;
    }

    public void setIsValueUnique(final Boolean isValueUnique) {
        this.isValueUnique = isValueUnique;
    }

    public Boolean getCanBeOrdered() {
        return canBeOrdered;
    }

    public void setCanBeOrdered(final Boolean canBeOrdered) {
        this.canBeOrdered = canBeOrdered;
    }

    public Boolean getUseSpecification() {
        return useSpecification;
    }

    public void setUseSpecification(final Boolean useSpecification) {
        this.useSpecification = useSpecification;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    public Boolean getFaOnly() {
        return faOnly;
    }

    public void setFaOnly(final Boolean faOnly) {
        this.faOnly = faOnly;
    }
}
