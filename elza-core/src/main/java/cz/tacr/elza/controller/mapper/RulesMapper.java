package cz.tacr.elza.controller.mapper;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.controller.vo.DataType;
import cz.tacr.elza.controller.vo.IntViewDefinition;
import cz.tacr.elza.controller.vo.ItemDisplayType;
import cz.tacr.elza.controller.vo.ItemTableColumn;
import cz.tacr.elza.controller.vo.ItemType;
import cz.tacr.elza.controller.vo.ItemTypeList;
import cz.tacr.elza.controller.vo.ItemTypeSpec;
import cz.tacr.elza.controller.vo.ItemViewDefinition;
import cz.tacr.elza.controller.vo.JsonTableViewDefinition;
import cz.tacr.elza.controller.vo.StringViewDefinition;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.integer.DisplayType;
import cz.tacr.elza.domain.table.ElzaColumn;

/**
 * Maps internal rule-definition domain objects to the public
 * {@code /api/v1/rules/*} DTOs.
 */
@Component
public class RulesMapper {

    private static final Logger logger = LoggerFactory.getLogger(RulesMapper.class);

    private final StaticDataService staticDataService;

    @Autowired
    public RulesMapper(StaticDataService staticDataService) {
        this.staticDataService = staticDataService;
    }

    public ItemTypeList toItemTypeList(List<RulItemTypeExt> source) {
        List<ItemType> mapped = new ArrayList<>(source.size());
        for (RulItemTypeExt src : source) {
            mapped.add(toItemType(src));
        }
        return new ItemTypeList(mapped);
    }

    ItemType toItemType(RulItemTypeExt src) {
        DataType dataType = toDataType(src.getDataType().getCode());
        ItemType out = new ItemType(
                src.getItemTypeId(),
                src.getCode(),
                src.getName(),
                src.getShortcut(),
                dataType,
                Boolean.TRUE.equals(src.getCanBeOrdered()));
        out.setDescription(src.getDescription());
        out.setStructureTypeCode(resolveStructureTypeCode(src, dataType));
        out.setViewDefinition(toViewDefinition(src, dataType));
        if (src.getRulItemSpecList() != null && !src.getRulItemSpecList().isEmpty()) {
            out.setSpecs(toItemTypeSpecs(src.getRulItemSpecList()));
        }
        return out;
    }

    private List<ItemTypeSpec> toItemTypeSpecs(List<RulItemSpecExt> source) {
        List<ItemTypeSpec> mapped = new ArrayList<>(source.size());
        for (RulItemSpec src : source) {
            mapped.add(toItemTypeSpec(src));
        }
        return mapped;
    }

    ItemTypeSpec toItemTypeSpec(RulItemSpec src) {
        ItemTypeSpec out = new ItemTypeSpec(
                src.getItemSpecId(),
                src.getCode(),
                src.getName(),
                src.getShortcut());
        out.setDescription(src.getDescription());
        return out;
    }

    private String resolveStructureTypeCode(RulItemType src, DataType dataType) {
        if (dataType != DataType.STRUCTURED || src.getStructuredTypeId() == null) {
            return null;
        }
        StructType st = staticDataService.getData().getStructuredTypeById(src.getStructuredTypeId());
        return st != null ? st.getCode() : null;
    }

    ItemViewDefinition toViewDefinition(RulItemType src, DataType dataType) {
        try {
            switch (dataType) {
            case INT: {
                DisplayType raw = (DisplayType) src.getViewDefinition(RulItemType.DISPLAY_TYPE);
                // NUMBER is the implicit default — omit so clients fall back to it.
                return raw == DisplayType.DURATION
                        ? new IntViewDefinition(ItemDisplayType.DURATION, DataType.INT)
                        : null;
            }
            case STRING: {
                cz.tacr.elza.domain.viewDefinition.StringViewDefinition svd =
                        (cz.tacr.elza.domain.viewDefinition.StringViewDefinition)
                                src.getViewDefinition(RulItemType.STRING_VIEW_DEFINITION);
                return (svd != null && svd.getMask() != null && !svd.getMask().isEmpty())
                        ? new StringViewDefinition(svd.getMask(), DataType.STRING)
                        : null;
            }
            case JSON_TABLE: {
                @SuppressWarnings("unchecked")
                List<ElzaColumn> columns = (List<ElzaColumn>) src.getViewDefinition(RulItemType.ELZA_COLUMNS);
                return (columns != null && !columns.isEmpty())
                        ? new JsonTableViewDefinition(toTableColumns(columns), DataType.JSON_TABLE)
                        : null;
            }
            default:
                return null;
            }
        } catch (IllegalArgumentException e) {
            // Domain getter throws on malformed JSON in rul_item_type.view_definition.
            logger.warn("Item type {} has unparseable viewDefinition; omitting from response.", src.getCode(), e);
            return null;
        }
    }

    private List<ItemTableColumn> toTableColumns(List<?> columns) {
        List<ItemTableColumn> out = new ArrayList<>(columns.size());
        for (Object o : columns) {
            if (o instanceof ElzaColumn col) {
                ItemTableColumn mapped = new ItemTableColumn(
                        col.getCode(),
                        col.getName(),
                        col.getDataType() != null ? col.getDataType().name() : null);
                mapped.setWidth(col.getWidth());
                out.add(mapped);
            }
        }
        return out;
    }

    /**
     * Maps the domain data-type code to the public {@link DataType} enum.
     * The two enums share the same set of values; the mapping is therefore a
     * one-to-one name match. An unknown code indicates a domain/spec mismatch
     * and is treated as a programming error.
     */
    static DataType toDataType(String domainCode) {
        try {
            return DataType.valueOf(domainCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Unknown domain data type code: " + domainCode
                            + ". Public DataType enum is out of sync with cz.tacr.elza.core.data.DataType.",
                    e);
        }
    }
}
