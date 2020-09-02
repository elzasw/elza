package cz.tacr.elza.print.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // write value with specification
        SpecTitleSource specTitleSource = ctx.getSpecTitleSource();

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
                    String specName = specTitleSource.getValue(spec).toLowerCase();
                    if (ctx.getGroupBySpec()) {
                        specs.add(specName);
                        List<String> values = specValues.computeIfAbsent(specName, a -> new ArrayList<>());
                        values.add(value);
                    } else {
                        ctx.appendSpecWithValue(specName, value);
                    }
                }
            }
        }

        // process values with specification
        for (String specName : specs) {
            List<String> values = specValues.get(specName);
            ctx.appendSpecWithValues(specName, values);
        }
    }
}
