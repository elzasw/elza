package cz.tacr.elza.dataexchange.output.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.domain.ArrDescItem;

/**
 * Applying a filter to the description items of a level, as bulk actions see the level.
 */
class ApplyFilterDescItemsTest {

    @Test
    void emptyFilterKeepsTheSameList() {
        List<ArrDescItem> items = List.of(new ArrDescItem(), new ArrDescItem());

        assertThat(new ApplyFilter().apply(items)).isSameAs(items);
    }

    @Test
    void hiddenLevelHasNoItems() {
        ApplyFilter filter = new ApplyFilter();
        filter.hideLevel();

        assertThat(filter.apply(List.of(new ArrDescItem()))).isNull();
    }

    @Test
    void hiddenItemsAreRemovedAndAddedItemsAppended() {
        ArrDescItem visible = new ArrDescItem();
        ArrDescItem hidden = new ArrDescItem();
        ArrDescItem added = new ArrDescItem();
        List<ArrDescItem> items = List.of(visible, hidden);

        ApplyFilter filter = new ApplyFilter();
        filter.addHideItem(hidden);
        filter.addItem(added);

        assertThat(filter.apply(items)).containsExactly(visible, added);
        // the level's own list is not changed
        assertThat(items).containsExactly(visible, hidden);
    }
}
