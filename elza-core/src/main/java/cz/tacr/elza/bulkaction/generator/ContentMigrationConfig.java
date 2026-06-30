package cz.tacr.elza.bulkaction.generator;

import java.util.Collections;
import java.util.List;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkActionDFS;

/**
 * Configuration of {@link ContentMigration}.
 *
 * The action holds an ordered list of {@link Rule rules}. Each rule moves the
 * value of a source item type into a target item type when its conditions hold.
 * Rules are evaluated in order on every node, and each rule sees the effect of
 * the previous ones.
 *
 * Two guards are applied implicitly (derived from the data model, not from the
 * configuration):
 * <ul>
 * <li>a move applies only when the target holds no value on the node (no merge,
 * no overwrite, idempotent re-runs),</li>
 * <li>a value is moved into a STRING target only when it is single-line;
 * multi-line values are left in the source (STRING cannot hold line breaks).</li>
 * </ul>
 */
public class ContentMigrationConfig extends BaseActionConfig {

    private List<Rule> rules = Collections.emptyList();

    /**
     * A conditional move from {@link #source} to {@link #target}.
     */
    public static class Rule {

        private String source;

        private String target;

        private List<Condition> conditions = Collections.emptyList();

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public List<Condition> getConditions() {
            return conditions;
        }

        public void setConditions(List<Condition> conditions) {
            this.conditions = conditions;
        }
    }

    /**
     * A predicate over the items of {@link #itemType} on the node. Exactly one of
     * {@link #maxLength} / {@link #count} must be set.
     */
    public static class Condition {

        private String itemType;

        /**
         * Holds only when every item of {@link #itemType} is at most this long.
         */
        private Integer maxLength;

        /**
         * Holds only when the number of items of {@link #itemType} equals this value.
         */
        private Integer count;

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public Integer getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(Integer maxLength) {
            this.maxLength = maxLength;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    @Override
    public BulkActionDFS createBulkAction() {
        return new ContentMigration(this);
    }
}
