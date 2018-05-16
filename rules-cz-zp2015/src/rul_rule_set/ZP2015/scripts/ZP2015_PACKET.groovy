import cz.tacr.elza.domain.ArrData
import cz.tacr.elza.domain.ArrStructuredItem
import org.apache.commons.lang3.StringUtils

List<ArrStructuredItem> items = ITEMS
int packetLeadingZeros = PACKET_LEADING_ZEROS
return toString(items, packetLeadingZeros)

static String toString(List<ArrStructuredItem> items, int packetLeadingZeros) {
    StringBuilder result = new StringBuilder()    
    addNotEmpty(result, toStringValue(items, "ZP2015_PACKET_PREFIX"))
    addNotEmpty(result, addZerosBefore(toStringValue(items, "ZP2015_PACKET_NUMBER"), packetLeadingZeros))
    addNotEmpty(result, toStringValue(items, "ZP2015_PACKET_POSTFIX"))
    return result.toString().trim()
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
    for (ArrStructuredItem item : items) {
        if (item.getItemType().getCode().equalsIgnoreCase(itemTypeCode)) {
            ArrData data = item.getData();
            if(data==null) {
                return null;
            }
            return data.getFulltextValue();
        }
    }
    return null;
}
