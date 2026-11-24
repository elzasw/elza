package cz.tacr.elza.bulkaction.generator;

import java.util.List;
import java.util.Objects;

import cz.tacr.elza.dataexchange.output.filters.ApplyFilter;
import cz.tacr.elza.dataexchange.output.filters.NodeRestrictionEvaluator;
import cz.tacr.elza.domain.ArrDescItem;
import jakarta.annotation.Nullable;

/**
 * Shows levels to a bulk action the way an output with the filter shows them: a hidden level
 * (with its whole subtree) is left out, the other levels get their items changed by the filter.
 *
 * The parent of a level has to be filtered before the level itself.
 */
class LevelOutputFilter {

    private final NodeRestrictionEvaluator restrictionEvaluator;

    LevelOutputFilter(final NodeRestrictionEvaluator restrictionEvaluator) {
        this.restrictionEvaluator = Objects.requireNonNull(restrictionEvaluator);
    }

    /**
     * @param level
     *            level with its own items; its parent is the level returned for the parent by this
     *            method, or the unfiltered parent when the parent is hidden
     * @return the level as the output shows it, null when the filter hides it
     */
    @Nullable
    LevelWithItems filter(LevelWithItems level) {
        LevelWithItems parent = level.getParent();
        Integer parentNodeId = parent != null ? parent.getNodeId() : null;
        ApplyFilter filter = restrictionEvaluator.evaluate(level.getNodeId(), parentNodeId, level.getDescItems());

        List<ArrDescItem> visibleItems = filter.apply(level.getDescItems());
        if (visibleItems == null) {
            return null;
        }
        if (visibleItems == level.getDescItems()) {
            return level;
        }
        return new LevelWithItems(level.getTreeNode(), parent, visibleItems);
    }
}
