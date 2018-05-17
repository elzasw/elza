import cz.tacr.elza.domain.ArrData
import cz.tacr.elza.domain.ArrStructuredItem
import cz.tacr.elza.domain.RulItemSpec
import org.apache.commons.lang3.StringUtils

List<ArrStructuredItem> items = ITEMS
int packetLeadingZeros = PACKET_LEADING_ZEROS
return toString(items, packetLeadingZeros)

static String toString(List<ArrStructuredItem> items, int packetLeadingZeros) {
    StringBuilder result = new StringBuilder();
    appendValue(result, items, "ZP2015_PACKET_FIXED_PREFIX");
    appendValue(result, items, "ZP2015_PACKET_PREFIX");
    addNotEmpty(result, addZerosBefore(toStringValue(items, "ZP2015_PACKET_NUMBER"), packetLeadingZeros))
    appendValue(result, items, "ZP2015_PACKET_POSTFIX");
    return result.toString().trim();
}

static void addNotEmpty(StringBuilder result, String value) {
    if (StringUtils.isNotBlank(value)) {
        result.append(value);
    }
}

static void addNotEmpty(StringBuilder result, String value, String valuePostfix) {
    if (StringUtils.isNotBlank(value)) {
        result.append(value).append(valuePostfix);
    }
}

static String addZerosBefore(String value, int totalLength) {
    if (value == null) {
        return null;
    }
    int addZeros = totalLength - value.length();
    if (addZeros > 0) {
        return StringUtils.repeat("0", addZeros) + value;
    } else {
        return value;
    }
}

static String toStringValue(List<ArrStructuredItem> items, String itemTypeCode) {
    StringBuilder result = new StringBuilder()    
    appendValue(result, items, itemTypeCode);
    return result.toString();
}

static void appendValue(StringBuilder result, List<ArrStructuredItem> items, String itemTypeCode)
{
    for (ArrStructuredItem item : items) {
        if (item.getItemType().getCode().equalsIgnoreCase(itemTypeCode)) {
            RulItemSpec spec = item.getItemSpec();
            if(spec!=null) {
                result.append(spec.getShortcut());
            }
            ArrData data = item.getData();
            if(data!=null) {
                result.append(data.getFulltextValue());
            }
        }
    }
}
