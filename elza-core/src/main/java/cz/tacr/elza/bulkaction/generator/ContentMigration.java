package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.BulkActionDFS;
import cz.tacr.elza.bulkaction.generator.ContentMigrationConfig.Condition;
import cz.tacr.elza.bulkaction.generator.ContentMigrationConfig.Rule;
import cz.tacr.elza.bulkaction.generator.result.ContentMigrationResult;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;

/**
 * Bulk action migrating description-item values between item types.
 *
 * Each configured rule moves the value of a source item type into a target item
 * type when its conditions hold. Rules run in order on every node, each seeing
 * the effect of the previous ones (so a rule can fill a target that an earlier
 * rule emptied).
 *
 * Two guards are implicit:
 * <ul>
 * <li>a move applies only when the target holds no value (no merge / overwrite,
 * idempotent),</li>
 * <li>only single-line values are moved into a STRING target; multi-line values
 * stay in the source.</li>
 * </ul>
 */
@Component
@Scope("prototype")
public class ContentMigration extends BulkActionDFS {

    private final ContentMigrationConfig config;

    private final List<ResolvedRule> rules = new ArrayList<>();

    private int movedItems = 0;

    public ContentMigration(ContentMigrationConfig config) {
        this.config = config;
    }

    @Override
    protected void init(ArrBulkActionRun bulkActionRun) {
        super.init(bulkActionRun);

        for (Rule rule : config.getRules()) {
            RulItemType source = resolveType(rule.getSource());
            RulItemType target = resolveType(rule.getTarget());
            requireTextLike(source);
            DataType targetDataType = requireTextLike(target);

            List<ResolvedCondition> conditions = new ArrayList<>();
            for (Condition condition : rule.getConditions()) {
                RulItemType condType = resolveType(condition.getItemType());
                boolean hasMaxLength = condition.getMaxLength() != null;
                boolean hasCount = condition.getCount() != null;
                if (hasMaxLength == hasCount) {
                    throw createConfigException("Condition must set exactly one of maxLength / count.")
                            .set("itemTypeCode", condType.getCode());
                }
                conditions.add(new ResolvedCondition(condType, condition.getMaxLength(), condition.getCount()));
            }
            rules.add(new ResolvedRule(source, target, targetDataType, conditions));
        }
    }

    private RulItemType resolveType(String code) {
        ItemType itemType = staticDataProvider.getItemTypeByCode(code);
        if (itemType == null) {
            throw createConfigException("Unknown item type.").set("itemTypeCode", code);
        }
        return itemType.getEntity();
    }

    private DataType requireTextLike(RulItemType itemType) {
        DataType dataType = DataType.fromId(itemType.getDataTypeId());
        if (dataType != DataType.STRING && dataType != DataType.TEXT) {
            throw createConfigException("Only STRING / TEXT item types are supported.")
                    .set("itemTypeCode", itemType.getCode());
        }
        return dataType;
    }

    @Override
    protected void update(ArrLevel level) {
        ArrNode node = level.getNode();
        // rules run without a batch change context, so each move is flushed immediately
        // and a later rule sees the updated node state (e.g. an emptied target)
        for (ResolvedRule rule : rules) {
            applyRule(node, rule);
        }
    }

    private boolean applyRule(ArrNode node, ResolvedRule rule) {
        // implicit guard: never merge into / overwrite an occupied target
        if (!loadItems(node, rule.target).isEmpty()) {
            return false;
        }
        List<ArrDescItem> sources = loadItems(node, rule.source);
        if (sources.isEmpty()) {
            return false;
        }
        for (ResolvedCondition condition : rule.conditions) {
            if (!evaluate(node, condition)) {
                return false;
            }
        }

        boolean moved = false;
        for (ArrDescItem source : sources) {
            String value = readValue(source);
            if (value == null) {
                continue;
            }
            // implicit guard: a STRING target cannot hold line breaks
            if (rule.targetDataType == DataType.STRING && !isSingleLine(value)) {
                continue;
            }
            createItem(node, rule.target, rule.targetDataType, value);
            deleteDescItem(getFondsVersion(), source);
            movedItems++;
            moved = true;
        }
        return moved;
    }

    private boolean evaluate(ArrNode node, ResolvedCondition condition) {
        List<ArrDescItem> items = loadItems(node, condition.itemType);
        if (condition.maxLength != null) {
            for (ArrDescItem item : items) {
                String value = readValue(item);
                if (value != null && value.length() > condition.maxLength) {
                    return false;
                }
            }
            return true;
        }
        // count predicate
        return items.size() == condition.count;
    }

    private List<ArrDescItem> loadItems(ArrNode node, RulItemType itemType) {
        return descriptionItemService.findByNodeAndDeleteChangeIsNullAndItemTypeId(node, itemType.getItemTypeId());
    }

    private void createItem(ArrNode node, RulItemType itemType, DataType dataType, String value) {
        ArrDescItem descItem = new ArrDescItem();
        descItem.setItemType(itemType);
        descItem.setNode(node);
        descItem.setData(makeData(dataType, value));
        saveNewDescItem(getFondsVersion(), descItem);
    }

    private static ArrData makeData(DataType dataType, String value) {
        if (dataType == DataType.STRING) {
            ArrDataString data = new ArrDataString();
            data.setStringValue(value);
            return data;
        }
        ArrDataText data = new ArrDataText();
        data.setTextValue(value);
        return data;
    }

    private static String readValue(ArrDescItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        if (data instanceof ArrDataText text) {
            return text.getTextValue();
        }
        if (data instanceof ArrDataString string) {
            return string.getStringValue();
        }
        return null;
    }

    private static boolean isSingleLine(String value) {
        return value.indexOf('\n') < 0 && value.indexOf('\r') < 0;
    }

    @Override
    protected void done() {
        ContentMigrationResult res = new ContentMigrationResult();
        res.setMovedItems(movedItems);
        result.getResults().add(res);
    }

    @Override
    public String getName() {
        return ContentMigration.class.getSimpleName();
    }

    private static final class ResolvedRule {
        final RulItemType source;
        final RulItemType target;
        final DataType targetDataType;
        final List<ResolvedCondition> conditions;

        ResolvedRule(RulItemType source, RulItemType target, DataType targetDataType,
                     List<ResolvedCondition> conditions) {
            this.source = source;
            this.target = target;
            this.targetDataType = targetDataType;
            this.conditions = conditions;
        }
    }

    private static final class ResolvedCondition {
        final RulItemType itemType;
        final Integer maxLength;
        final Integer count;

        ResolvedCondition(RulItemType itemType, Integer maxLength, Integer count) {
            this.itemType = itemType;
            this.maxLength = maxLength;
            this.count = count;
        }
    }
}
