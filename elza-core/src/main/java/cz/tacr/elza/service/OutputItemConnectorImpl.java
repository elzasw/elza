package cz.tacr.elza.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.table.ElzaTable;

public class OutputItemConnectorImpl implements OutputItemConnector {

    private static final Logger logger = LoggerFactory.getLogger(OutputItemConnectorImpl.class);

    private final Set<Integer> deleteItemTypeIds = new HashSet<>();

    private final ArrFundVersion fundVersion;

    private final ArrOutputDefinition outputDefinition;

    private Supplier<ArrChange> changeSupplier;

    private Set<Integer> ignoredItemTypeIds;

    private Integer allowedItemTypeId;

    /* managed components */

    private final StaticDataService staticDataService;

    private final OutputServiceInternal outputServiceInternal;

    private final ItemService itemService;

    public OutputItemConnectorImpl(ArrFundVersion fundVersion,
                                   ArrOutputDefinition outputDefinition,
                                   StaticDataService staticDataService,
                                   OutputServiceInternal outputServiceInternal,
                                   ItemService itemService) {
        this.fundVersion = fundVersion;
        this.outputDefinition = outputDefinition;
        this.staticDataService = staticDataService;
        this.outputServiceInternal = outputServiceInternal;
        this.itemService = itemService;
    }

    public void setIgnoredItemTypeIds(Collection<Integer> ignoredItemTypeIds) {
        Validate.isTrue(this.ignoredItemTypeIds == null);

        this.ignoredItemTypeIds = new HashSet<>(ignoredItemTypeIds);
    }

    @Override
    public void setItemTypeFilter(int allowedItemTypeId) {
        Validate.isTrue(this.allowedItemTypeId == null);

        this.allowedItemTypeId = allowedItemTypeId;

    }

    @Override
    public RuleSystem getRuleSystem() {
        return staticDataService.getData().getRuleSystems().getByRuleSetId(fundVersion.getRuleSetId());
    }

    @Override
    public void setChangeSupplier(Supplier<ArrChange> changeSupplier) {
        Validate.isTrue(this.changeSupplier == null);

        this.changeSupplier = changeSupplier;
    }

    @Override
    public void addIntItem(int value, RuleSystemItemType rsit, Integer itemSpecId) {
        Validate.isTrue(rsit.getDataType() == DataType.INT);
        if (isItemTypeIgnored(rsit)) {
            return;
        }

        ArrDataInteger data = new ArrDataInteger();
        data.setDataType(rsit.getDataType().getEntity());
        data.setValue(value);
        addOutputItem(data, rsit, itemSpecId);
    }

    @Override
    public void addStringItem(String value, RuleSystemItemType rsit, Integer itemSpecId) {
        if (isItemTypeIgnored(rsit)) {
            return;
        }
        switch (rsit.getDataType()) {
            case STRING:
                ArrDataString str = new ArrDataString();
                str.setDataType(rsit.getDataType().getEntity());
                str.setValue(value);
                addOutputItem(str, rsit, itemSpecId);
                break;
            case TEXT:
            case FORMATTED_TEXT:
                ArrDataText txt = new ArrDataText();
                txt.setDataType(rsit.getDataType().getEntity());
                txt.setValue(value);
                addOutputItem(txt, rsit, itemSpecId);
                break;
            default:
                throw new IllegalArgumentException("Invalid string data type, itemType:" + rsit.getCode());
        }
    }

    @Override
    public void addTableItem(ElzaTable value, RuleSystemItemType rsit, Integer itemSpecId) {
        Validate.isTrue(rsit.getDataType() == DataType.JSON_TABLE);
        if (isItemTypeIgnored(rsit)) {
            return;
        }

        ArrDataJsonTable data = new ArrDataJsonTable();
        data.setDataType(rsit.getDataType().getEntity());
        data.setValue(value);
        addOutputItem(data, rsit, itemSpecId);
    }

    @Override
    public void addItems(Collection<ArrItem> items, RuleSystemItemType rsit) {
        // fetch data
        itemService.refItemsLoader(items);

        for (ArrItem item : items) {
            if (isItemTypeIgnored(rsit)) {
                continue;
            }
            RuleSystemItemType type = getRuleSystem().getItemTypeById(item.getItemTypeId());
            if (type.hasSpecifications()) {
                // check item type
                Validate.isTrue(type.getItemTypeId().equals(rsit.getItemTypeId()),
                        "Item type does not match, itemTypeId1:" + type.getItemTypeId() + ", itemTypeId2:" + rsit.getItemTypeId());
            } else {
                // check data type
                Validate.isTrue(type.getDataTypeId().equals(rsit.getDataTypeId()),
                        "Data type does not match, dataTypeId1:" + type.getDataTypeId() + ", dataTypeId2:" + rsit.getDataTypeId());
            }

            ArrData data = ArrData.makeCopyWithoutId(item.getData());

            addOutputItem(data, rsit, item.getItemSpecId());
        }
    }

    @Override
    public Set<Integer> getModifiedItemTypeIds() {
        return deleteItemTypeIds;
    }

    private void addOutputItem(ArrData data, RuleSystemItemType rsit, Integer itemSpecId) {
        // create output item
        ArrOutputItem outputItem = new ArrOutputItem();
        outputItem.setData(data);
        outputItem.setOutputDefinition(outputDefinition);
        outputItem.setItemType(rsit.getEntity());
        if (itemSpecId != null) {
            RulItemSpec spec = rsit.getItemSpecById(itemSpecId);
            Validate.notNull(spec);
            outputItem.setItemSpec(spec);
        }

        ArrChange change = changeSupplier.get();

        // check if not already deleted
        if (deleteItemTypeIds.add(rsit.getItemTypeId())) {
            outputServiceInternal.deleteOutputItemsByType(fundVersion, outputDefinition, rsit.getItemTypeId(), change);
        }

        outputServiceInternal.createOutputItem(outputItem, fundVersion, change);
    }

    private boolean isItemTypeIgnored(RuleSystemItemType rsit) {
        Integer itemTypeId = rsit.getItemTypeId();

        if (ignoredItemTypeIds != null && ignoredItemTypeIds.contains(itemTypeId)) {
            logger.warn("Output item " + outputDefinition.getName() + " [ID=" + outputDefinition.getOutputDefinitionId()
                    + "] with type " + rsit.getEntity().getName() + " [CODE=" + rsit.getCode()
                    + "] was skipped because the type was registered as ignored");
            return true;
        }
        if (allowedItemTypeId != null && !allowedItemTypeId.equals(itemTypeId)) {
            logger.warn("Output item " + outputDefinition.getName() + " [ID=" + outputDefinition.getOutputDefinitionId()
                    + "] with type " + rsit.getEntity().getName() + " [CODE=" + rsit.getCode()
                    + "] was skipped because the type was filtered out");
            return true;
        }

        return false;
    }
}
