package scripts

import groovy.transform.Field
import cz.tacr.elza.domain.ArrData
import cz.tacr.elza.domain.ArrStructuredItem
import cz.tacr.elza.domain.RulItemSpec
import org.apache.commons.lang3.StringUtils

/*
 * Value of a container: the kind, the prefix, the number and the postfix - "box 12", "folder 3a";
 * the sort value pads the number; the complement is the location.
 */

@Field def result = RESULT
@Field List<ArrStructuredItem> items = ITEMS
@Field static int leadingZeros = 8

generate()
return

void generate() {
    StringBuilder value = new StringBuilder()
    StringBuilder sortValue = new StringBuilder()

    String kind = shortcut("ISADG_CONTAINER_KIND")
    if (StringUtils.isNotBlank(kind)) {
        value.append(kind).append(" ")
        sortValue.append(kind).append(" ")
    }
    appendValue(value, "ISADG_CONTAINER_PREFIX")
    appendValue(sortValue, "ISADG_CONTAINER_PREFIX")

    String number = text("ISADG_CONTAINER_NUMBER")
    if (StringUtils.isNotBlank(number)) {
        int zeros = Container.leadingZeros - number.length()
        if (zeros > 0) {
            sortValue.append(StringUtils.repeat("0", zeros))
        }
        value.append(number)
        sortValue.append(number)
    }
    appendValue(value, "ISADG_CONTAINER_POSTFIX")
    appendValue(sortValue, "ISADG_CONTAINER_POSTFIX")

    result.setValue(value.toString().trim())
    result.setSortValue(sortValue.toString().trim())
    String location = text("ISADG_CONTAINER_LOCATION").trim()
    result.setComplement(StringUtils.isEmpty(location) ? null : location)
}

String shortcut(String itemTypeCode) {
    for (ArrStructuredItem item : items) {
        if (item.getItemType().getCode().equals(itemTypeCode)) {
            RulItemSpec spec = item.getItemSpec()
            return spec == null ? "" : spec.getShortcut()
        }
    }
    return ""
}

String text(String itemTypeCode) {
    StringBuilder sb = new StringBuilder()
    appendValue(sb, itemTypeCode)
    return sb.toString()
}

void appendValue(StringBuilder sb, String itemTypeCode) {
    for (ArrStructuredItem item : items) {
        if (item.getItemType().getCode().equals(itemTypeCode)) {
            ArrData data = item.getData()
            if (data != null) {
                sb.append(data.getFulltextValue())
            }
        }
    }
}
