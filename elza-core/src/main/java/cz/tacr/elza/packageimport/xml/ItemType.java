package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO ItemType from XML
 *
 * View order is based on position in the list
 * @author Martin Šlapa
 * @author Petr Pytelka
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item-type")
public class ItemType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "data-type", required = true)
    private String dataType;

    @XmlAttribute(name = "structure-type")
    private String structureType;

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

    @XmlElement(name = "column")
    @XmlElementWrapper(name = "columns-definitions")
    private List<Column> columnsDefinition;

    @XmlElement(name = "display-type")
    private DisplayType displayType;

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

    public String getStructureType() {
        return structureType;
    }

    public void setStructureType(final String structureType) {
        this.structureType = structureType;
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

    public List<Column> getColumnsDefinition() {
        return columnsDefinition;
    }

    public void setColumnsDefinition(final List<Column> columnsDefinition) {
        this.columnsDefinition = columnsDefinition;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(DisplayType displayType) {
        this.displayType = displayType;
    }
}
