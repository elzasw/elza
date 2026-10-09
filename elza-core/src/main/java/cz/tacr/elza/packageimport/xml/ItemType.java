package cz.tacr.elza.packageimport.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.RulItemAptype;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeDeclaration;
import cz.tacr.elza.domain.viewDefinition.StringViewDefinition;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.repository.ItemAptypeRepository;

/**
 * VO ItemType from XML
 *
 * View order is based on position in the list
 *
 * @author Martin Šlapa
 * @author Petr Pytelka
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item-type")
public class ItemType {

    // --- fields ---

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

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "can-be-ordered")
    private Boolean canBeOrdered;

    @XmlElement(name = "use-specification", required = true)
    private Boolean useSpecification;

    @XmlElement(name="string-length-limit")
    private Integer stringLengthLimit;

    @XmlElement(name = "column")
    @XmlElementWrapper(name = "columns-definitions")
    private List<Column> columnsDefinition;

    @XmlElement(name = "display-type")
    private DisplayType displayType;
    
    @XmlElement(name = "mask")
    private String mask;

    @XmlElement(name = "item-aptype")
    @XmlElementWrapper(name = "item-aptypes")
    private List<ItemAptype> itemAptypes;

    // --- getters/setters ---

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

    public String getMask() {
		return mask;
	}

	public void setMask(String mask) {
		this.mask = mask;
	}

	public List<ItemAptype> getItemAptypes() {
        return itemAptypes;
    }

    public void setItemAptypes(List<ItemAptype> itemAptypes) {
        this.itemAptypes = itemAptypes;
    }

    public Integer getStringLengthLimit() {
        return stringLengthLimit;
    }

    public void setStringLengthLimit(Integer stringLengthLimit) {
        this.stringLengthLimit = stringLengthLimit;
    }

    // --- methods ---

    /**
     * Převod DAO na VO typů atributu.
     *
     * @param rulDescItemType DAO typy
     * @param itemType VO typu
     */
    /**
     * The item type as the package declares it: the stored data (data type, use of specifications,
     * structured type, columns) from the item type, texts and the other values from the declaration.
     *
     * @param itemAptypes
     *            RECORD_REF classes, written for the owner of the item type only
     */
    public static ItemType fromDeclaration(RulItemType rulItemType, RulItemTypeDeclaration declaration,
                                           List<RulItemAptype> itemAptypes) {
        RulItemType declared = new RulItemType();
        declared.setCode(rulItemType.getCode());
        declared.setDataType(rulItemType.getDataType());
        declared.setUseSpecification(rulItemType.getUseSpecification());
        declared.setStructuredType(rulItemType.getStructuredType());
        declared.setName(declaration.getName());
        declared.setShortcut(declaration.getShortcut());
        declared.setDescription(declaration.getDescription());
        declared.setCanBeOrdered(declaration.getCanBeOrdered());
        declared.setStringLengthLimit(declaration.getStringLengthLimit());
        declared.setViewDefinitionJson(declaration.getViewDefinition());
        return fromEntity(declared, itemAptypes);
    }

    public static ItemType fromEntity(RulItemType rulDescItemType, ItemAptypeRepository itemAptypeRepository) {
        return fromEntity(rulDescItemType, itemAptypeRepository.findByItemType(rulDescItemType));
    }

    private static ItemType fromEntity(RulItemType rulDescItemType, List<RulItemAptype> itemAptypes) {

        ItemType itemType = new ItemType();
        itemType.setCode(rulDescItemType.getCode());
        itemType.setName(rulDescItemType.getName());
        itemType.setShortcut(rulDescItemType.getShortcut());
        itemType.setCanBeOrdered(rulDescItemType.getCanBeOrdered());
        itemType.setDataType(rulDescItemType.getDataType().getCode());
        itemType.setDescription(rulDescItemType.getDescription());
        itemType.setUseSpecification(rulDescItemType.getUseSpecification());
        itemType.setStringLengthLimit(rulDescItemType.getStringLengthLimit());

        DataType dataType = DataType.fromCode(rulDescItemType.getDataType().getCode());

        if (dataType == DataType.JSON_TABLE) {
            List<ElzaColumn> columnsDefinition = (List<ElzaColumn>) rulDescItemType.getViewDefinition();
            if (columnsDefinition != null) {
                List<Column> columns = new ArrayList<>(columnsDefinition.size());
                for (ElzaColumn elzaColumn : columnsDefinition) {
                    Column column = new Column();
                    column.setCode(elzaColumn.getCode());
                    column.setName(elzaColumn.getName());
                    column.setDataType(elzaColumn.getDataType().name());
                    column.setWidth(elzaColumn.getWidth());
                    columns.add(column);
                }
                itemType.setColumnsDefinition(columns);
            }
        } else if (dataType == DataType.INT) {
            cz.tacr.elza.domain.integer.DisplayType displayType = (cz.tacr.elza.domain.integer.DisplayType) rulDescItemType
                    .getViewDefinition();
            if (displayType != null) {
                itemType.displayType = cz.tacr.elza.packageimport.xml.DisplayType.valueOf(displayType.name());
            }
        } else if (dataType == DataType.STRING) {
            StringViewDefinition stringViewDefinition = (StringViewDefinition) rulDescItemType.getViewDefinition();
            if (stringViewDefinition != null) {
                itemType.setMask(stringViewDefinition.getMask());
            }
        }
        if (rulDescItemType.getStructuredType() != null) {
            itemType.setStructureType(rulDescItemType.getStructuredType().getCode());
        }

        if (!itemAptypes.isEmpty()) {
            itemType.setItemAptypes(itemAptypes.stream().map(ItemAptype::fromEntity).collect(Collectors.toList()));
        }

        return itemType;
    }
}
