import cz.tacr.elza.domain.ArrStructureItem
import org.apache.commons.lang3.StringUtils

List<ArrStructureItem> items = ITEMS
int packetNumberLength = PACKET_NUMBER_LENGTH
return toString(items, packetNumberLength)

static String toString(List<ArrStructureItem> items, int packetNumberLength) {
    List<String> result = new ArrayList<>()
    addNotEmpty(result, toStringValue(items, "ZP2015_PACKET_TYPE"), " ")
    addNotEmpty(result, toStringValue(items, "ZP2015_PACKET_PREFIX"))
    addNotEmpty(result, addZerosBefore(toStringValue(items, "ZP2015_PACKET_NUMBER"), packetNumberLength))
    addNotEmpty(result, toStringValue(items, "ZP2015_PACKET_POSTFIX"))
    return String.join("", result).trim()
}

static void addNotEmpty(List<String> values, String value) {
    if (StringUtils.isNotBlank(value)) {
        values.add(value)
    }
}

static void addNotEmpty(List<String> values, String value, String valuePostfix) {
    if (StringUtils.isNotBlank(value)) {
        values.add(value + valuePostfix)
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

static String toStringValue(List<ArrStructureItem> items, String itemTypeCode) {
    for (ArrStructureItem item : items) {
        if (item.getItemType().getCode().equalsIgnoreCase(itemTypeCode)) {
            return item.getFulltextValue();
        }
    }
    return null;
}
