package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.RulItemType;

import java.util.List;

/**
 * VO typu hodnoty atributu
 *
 * @author Martin Šlapa
 * @since 13.1.2016
 */
public class RulDescItemTypeVO {

    /**
     * identifikátor typu
     */
    private Integer id;

    /**
     * identifikátor datového typu
     */
    private Integer dataTypeId;

    /**
     * kód
     */
    private String code;

    /**
     * název
     */
    private String name;

    /**
     * zkratka
     */
    private String shortcut;

    /**
     * popis
     */
    private String description;

    /**
     * je hodnota unikátní?
     */
    private Boolean isValueUnique;

    /**
     * může se řadit?
     */
    private Boolean canBeOrdered;

    /**
     * použít specifikaci?
     */
    private Boolean useSpecification;

    /**
     * řazení ve formuláři jednotky popisu
     */
    private Integer viewOrder;

    /**
     * typ důležitosti
     */
    @Deprecated
    private RulItemType.Type type;

    /**
     * opakovatelnost
     */
    @Deprecated
    private Boolean repeatable;

    private Object viewDefinition;

    /**
     * Kategorie specifikací.
     */
    private List<TreeItemSpecsItem> itemSpecsTree;

    /**
     * šířka atributu (0 - maximální počet sloupců, 1..N - počet sloupců)
     */
    private Integer width;

    /**
     * identifikátor strukturovaného typu
     */
    private Integer structureTypeId;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getDataTypeId() {
        return dataTypeId;
    }

    public void setDataTypeId(final Integer dataTypeId) {
        this.dataTypeId = dataTypeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
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

    public RulItemType.Type getType() {
        return type;
    }

    public void setType(final RulItemType.Type type) {
        this.type = type;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(final Integer width) {
        this.width = width;
    }

    public Object getViewDefinition() {
        return viewDefinition;
    }

    public void setViewDefinition(final Object viewDefinition) {
        this.viewDefinition = viewDefinition;
    }

    public List<TreeItemSpecsItem> getItemSpecsTree() {
        return itemSpecsTree;
    }

    public void setItemSpecsTree(final List<TreeItemSpecsItem> itemSpecsTree) {
        this.itemSpecsTree = itemSpecsTree;
    }

    public Integer getStructureTypeId() {
        return structureTypeId;
    }

    public void setStructureTypeId(Integer structureTypeId) {
        this.structureTypeId = structureTypeId;
    }
}
