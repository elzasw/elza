package scripts

import cz.tacr.elza.service.vo.SimpleItem

List<SimpleItem> items = ITEMS
return toString(items)

static String toString(List<SimpleItem> items) {
    StringBuilder result = new StringBuilder()
    boolean add = appendValue(result, items, "VZTAH_TYP")
    if (add) {
        result.append(": ")
    }
    appendValue(result, items, "VZTAH_ENTITA")
    return result.toString().trim()
}

static boolean appendValue(StringBuilder result, List<SimpleItem> items, String itemTypeCode) {
    int add = 0;
    for (SimpleItem item : items) {
        if (item.getType().equalsIgnoreCase(itemTypeCode)) {
            if (add > 0) {
                result.append(", ")
            }
            result.append(item.getValue())
            add++
        }
    }
    return add > 0
}
