package cz.tacr.elza.print.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;

/**
 * Format value of one item.
 */
public class ValueFormatter implements FormatAction {
    /**
     * Code of item to format
     */
    private final String itemType;

    ValueFormatter(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        List<String> specs = new ArrayList<>();
        Map<String, List<String>> specValues = new HashMap<>();

        for (Item item : items) {
            if (item.getType().getCode().equals(itemType)) {

                String value = item.getSerializedValue();
                ItemSpec spec = item.getSpecification();
                if (spec == null) {
                    // write value without specification
                    ctx.appendValue(value);
                } else {
                    // store spec value for later processing
                    String specName = ctx.getSpecName(spec).toLowerCase();
                    if (ctx.getGroupBySpec()) {
                        List<String> values = specValues.computeIfAbsent(specName, a -> new ArrayList<>());
                        if (values.size() == 0) {
                            // new spec found -> add it to the collection
                            specs.add(specName);
                        }
                        values.add(value);
                    } else {
                        ctx.appendSpecWithValue(specName, value);
                    }
                }
            }
        }

        // process values with same specification
        for (String specName : specs) {
            List<String> values = specValues.get(specName);
            ctx.appendSpecWithValues(specName, values);
        }
    }
}
