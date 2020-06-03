package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.domain.ArrStructuredItem;

import java.util.List;

public class AccessPointAggregationStructResult {

    private List<ArrStructuredItem> items;

    public List<ArrStructuredItem> getItems() {
        return items;
    }

    public void setItems(final List<ArrStructuredItem> items) {
        this.items = items;
    }
}
